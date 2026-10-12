package com.teammatch.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.teammatch.backend.config.PasswordEncoderConfiguration;
import com.teammatch.backend.config.SecurityConfiguration;
import com.teammatch.backend.dto.StudentResponse;
import com.teammatch.backend.entity.enums.AccountRole;
import com.teammatch.backend.security.TeamMatchPrincipal;
import com.teammatch.backend.service.StudentService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(AuthenticationController.class)
@Import({ SecurityConfiguration.class, PasswordEncoderConfiguration.class })
class AuthenticationControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthenticationManager authenticationManager;

	@MockitoBean
	private StudentService studentService;

	@Test
	void publicLoginWithoutExistingSessionPersistsStudentAuthentication() throws Exception {
		TeamMatchPrincipal principal = principal(5L);
		Authentication studentAuthentication = studentAuthentication(principal);
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenReturn(studentAuthentication);
		when(studentService.findById(5L)).thenReturn(studentResponse());

		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
					.with(csrf())
					.contentType("application/json")
					.content("{\"email\":\"student5@example.com\",\"password\":\"correct-password\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(5))
				.andExpect(jsonPath("$.email").value("student5@example.com"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andReturn();

		MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(5));
	}

	@Test
	void configuredAdministratorCanSignInWithAConfiguredUsername() throws Exception {
		TeamMatchPrincipal principal = mock(TeamMatchPrincipal.class);
		when(principal.getRole()).thenReturn(AccountRole.ADMIN);
		when(principal.getStudentId()).thenReturn(null);
		when(principal.getUsername()).thenReturn("team-admin");
		org.mockito.Mockito.doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
				.when(principal).getAuthorities();
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenReturn(UsernamePasswordAuthenticationToken.authenticated(
						principal, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

		mockMvc.perform(post("/api/auth/login")
					.with(csrf())
					.contentType("application/json")
					.content("{\"email\":\"team-admin\",\"password\":\"configured-secret\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accountRole").value("ADMIN"))
				.andExpect(jsonPath("$.email").value("team-admin"))
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void publicLoginWithoutExistingSessionRejectsIncorrectCredentials() throws Exception {
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenThrow(new BadCredentialsException("Bad credentials"));

		mockMvc.perform(post("/api/auth/login")
					.with(csrf())
					.contentType("application/json")
					.content("{\"email\":\"student5@example.com\",\"password\":\"incorrect-password\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Email or password is incorrect."));
	}

	@Test
	void sharedDevelopmentIdentityCannotSignInAsAStudent() throws Exception {
		TeamMatchPrincipal devPrincipal = mock(TeamMatchPrincipal.class);
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenReturn(UsernamePasswordAuthenticationToken.authenticated(
						devPrincipal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

		mockMvc.perform(post("/api/auth/login")
					.with(csrf())
					.contentType("application/json")
					.content("{\"email\":\"development-user@example.com\",\"password\":\"dev-password\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void logoutInvalidatesSessionAuthentication() throws Exception {
		TeamMatchPrincipal principal = principal(5L);
		Authentication studentAuthentication = studentAuthentication(principal);
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenReturn(studentAuthentication);
		when(studentService.findById(5L)).thenReturn(studentResponse());
		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
					.with(csrf())
					.contentType("application/json")
					.content("{\"email\":\"student5@example.com\",\"password\":\"correct-password\"}"))
				.andExpect(status().isOk())
				.andReturn();
		MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

		mockMvc.perform(post("/api/auth/logout").session(session).with(csrf()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void currentStudentRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
	}

	private TeamMatchPrincipal principal(Long studentId) {
		TeamMatchPrincipal principal = mock(TeamMatchPrincipal.class);
		when(principal.getStudentId()).thenReturn(studentId);
		when(principal.getUsername()).thenReturn("student" + studentId + "@example.com");
		when(principal.getPassword()).thenReturn("encoded-password-hash");
		when(principal.getRole()).thenReturn(AccountRole.STUDENT);
		org.mockito.Mockito.doReturn(List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
				.when(principal).getAuthorities();
		return principal;
	}

	private Authentication studentAuthentication(TeamMatchPrincipal principal) {
		return UsernamePasswordAuthenticationToken.authenticated(
				principal, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
	}

	private StudentResponse studentResponse() {
		return new StudentResponse(5L, "Student", "ROLL005", "student5@example.com", "CSE", 2,
				true, AccountRole.STUDENT);
	}
}

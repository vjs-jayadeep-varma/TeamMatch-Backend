package com.teammatch.backend.controller;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.teammatch.backend.config.DevSecurityConfiguration;
import com.teammatch.backend.config.PasswordEncoderConfiguration;
import com.teammatch.backend.dto.StudentInterestsRequest;
import com.teammatch.backend.dto.StudentInterestsResponse;
import com.teammatch.backend.entity.enums.AccountRole;
import com.teammatch.backend.repository.StudentRepository;
import com.teammatch.backend.security.TeamMatchPrincipal;
import com.teammatch.backend.service.StudentInterestService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StudentInterestController.class)
@Import({ DevSecurityConfiguration.class, PasswordEncoderConfiguration.class })
@ActiveProfiles("dev")
@TestPropertySource(properties = {
		"TEAMMATCH_DEV_USERNAME=test-user",
		"TEAMMATCH_DEV_PASSWORD=test-only-development-password"
})
class StudentInterestControllerTest {
	private static final String TEST_USERNAME = "test-user";
	private static final String TEST_PASSWORD = "test-only-development-password";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private StudentInterestService studentInterestService;

	@MockitoBean
	private StudentRepository studentRepository;

	@Test
	void returnsInterestsForAuthenticatedStudent() throws Exception {
		when(studentInterestService.findByStudentId(5L))
				.thenReturn(new StudentInterestsResponse(5L, List.of("Robotics")));

		mockMvc.perform(get("/api/students/5/interests").with(user(principal(5L))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.studentId").value(5))
				.andExpect(jsonPath("$.interests[0]").value("Robotics"));
	}

	@Test
	void replacesInterestsForAuthenticatedStudent() throws Exception {
		when(studentInterestService.replace(5L, new StudentInterestsRequest(List.of("Robotics"))))
				.thenReturn(new StudentInterestsResponse(5L, List.of("Robotics")));

		mockMvc.perform(put("/api/students/5/interests")
					.with(user(principal(5L)))
					.with(csrf())
					.contentType("application/json")
					.content("{\"interests\":[\"Robotics\"]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.interests[0]").value("Robotics"));
	}

	@Test
	void rejectsRequestsForAnotherStudent() throws Exception {
		mockMvc.perform(get("/api/students/6/interests").with(user(principal(5L))))
				.andExpect(status().isForbidden());

		mockMvc.perform(put("/api/students/6/interests")
					.with(user(principal(5L)))
					.with(csrf())
					.contentType("application/json")
					.content("{\"interests\":[]}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void rejectsSharedDevelopmentIdentity() throws Exception {
		mockMvc.perform(get("/api/students/5/interests")
					.with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
				.andExpect(status().isForbidden());
	}

	@Test
	void validatesInterestValues() throws Exception {
		mockMvc.perform(put("/api/students/5/interests")
					.with(user(principal(5L)))
					.with(csrf())
					.contentType("application/json")
					.content("{\"interests\":[\"\"]}"))
				.andExpect(status().isBadRequest());
	}

	private TeamMatchPrincipal principal(Long studentId) {
		TeamMatchPrincipal principal = mock(TeamMatchPrincipal.class);
		when(principal.getStudentId()).thenReturn(studentId);
		when(principal.getUsername()).thenReturn("student" + studentId + "@example.com");
		when(principal.getPassword()).thenReturn("encoded-password-hash");
		when(principal.getRole()).thenReturn(AccountRole.STUDENT);
		doReturn(List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))).when(principal).getAuthorities();
		return principal;
	}
}

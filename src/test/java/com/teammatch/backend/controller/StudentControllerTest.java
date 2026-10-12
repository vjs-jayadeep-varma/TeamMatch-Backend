package com.teammatch.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.teammatch.backend.config.DevSecurityConfiguration;
import com.teammatch.backend.config.PasswordEncoderConfiguration;
import com.teammatch.backend.config.SecurityConfiguration;
import com.teammatch.backend.dto.StudentCreateRequest;
import com.teammatch.backend.dto.StudentResponse;
import com.teammatch.backend.dto.StudentUpdateRequest;
import com.teammatch.backend.entity.enums.AccountRole;
import com.teammatch.backend.exception.DuplicateResourceException;
import com.teammatch.backend.repository.StudentRepository;
import com.teammatch.backend.security.TeamMatchPrincipal;
import com.teammatch.backend.service.StudentService;
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

@WebMvcTest(StudentController.class)
@Import({ DevSecurityConfiguration.class, PasswordEncoderConfiguration.class, SecurityConfiguration.class })
@ActiveProfiles("dev")
@TestPropertySource(properties = {
		"TEAMMATCH_DEV_USERNAME=test-user",
		"TEAMMATCH_DEV_PASSWORD=test-only-development-password"
})
class StudentControllerTest {
	private static final String TEST_USERNAME = "test-user";
	private static final String TEST_PASSWORD = "test-only-development-password";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private StudentService studentService;

	@MockitoBean
	private StudentRepository studentRepository;

	@Test
	void returnsOwnStudentProfile() throws Exception {
		when(studentService.findById(5L)).thenReturn(studentResponse(5L));

		mockMvc.perform(get("/api/students/5").with(user(principal(5L))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(5))
				.andExpect(jsonPath("$.email").value("student5@example.com"));
	}

	@Test
	void registrationIsPublicAndCreatesAStudentWithoutAnExistingLogin() throws Exception {
		when(studentService.create(any(StudentCreateRequest.class))).thenReturn(studentResponse(7L));

		mockMvc.perform(post("/api/students")
					.with(csrf())
					.contentType("application/json")
					.content("""
							{"name":"Student","rollNumber":"ROLL007","email":"student7@example.com",
							"password":"long-enough-password","department":"CSE","academicYear":2}
							"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(7))
				.andExpect(jsonPath("$.accountRole").value("STUDENT"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void registrationRejectsDuplicateAccounts() throws Exception {
		when(studentService.create(any(StudentCreateRequest.class)))
				.thenThrow(new DuplicateResourceException("A student with this email already exists."));

		mockMvc.perform(post("/api/students")
					.with(csrf())
					.contentType("application/json")
					.content("""
							{"name":"Student","rollNumber":"ROLL007","email":"student7@example.com",
							"password":"long-enough-password","department":"CSE","academicYear":2}
							"""))
				.andExpect(status().isConflict());
	}

	@Test
	void updatesOwnStudentProfile() throws Exception {
		when(studentService.update(org.mockito.ArgumentMatchers.eq(5L), any(StudentUpdateRequest.class)))
				.thenReturn(studentResponse(5L));

		mockMvc.perform(put("/api/students/5")
					.with(user(principal(5L)))
					.with(csrf())
					.contentType("application/json")
					.content("""
							{"name":"Student","rollNumber":"ROLL005","email":"student5@example.com",
							"department":"CSE","academicYear":2,"available":true}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(5));
	}

	@Test
	void rejectsCrossStudentProfileReadsAndUpdates() throws Exception {
		mockMvc.perform(get("/api/students/6").with(user(principal(5L))))
				.andExpect(status().isForbidden());
		mockMvc.perform(put("/api/students/6")
					.with(user(principal(5L)))
					.with(csrf())
					.contentType("application/json")
					.content("""
							{"name":"Student","rollNumber":"ROLL006","email":"student6@example.com",
							"department":"CSE","academicYear":2,"available":true}
							"""))
				.andExpect(status().isForbidden());
		verifyNoInteractions(studentService);
	}

	@Test
	void sharedDevelopmentUserCannotReadOrUpdateStudentProfiles() throws Exception {
		mockMvc.perform(get("/api/students/5")
					.with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
				.andExpect(status().isForbidden());
		mockMvc.perform(put("/api/students/5")
					.with(httpBasic(TEST_USERNAME, TEST_PASSWORD))
					.with(csrf())
					.contentType("application/json")
					.content("""
							{"name":"Student","rollNumber":"ROLL005","email":"student5@example.com",
							"department":"CSE","academicYear":2,"available":true}
							"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void studentProfileEndpointsRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/students/5"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(put("/api/students/5")
					.with(csrf())
					.contentType("application/json")
					.content("""
							{"name":"Student","rollNumber":"ROLL005","email":"student5@example.com",
							"department":"CSE","academicYear":2,"available":true}
							"""))
				.andExpect(status().isUnauthorized());
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

	private StudentResponse studentResponse(Long studentId) {
		return new StudentResponse(studentId, "Student", "ROLL00" + studentId,
				"student" + studentId + "@example.com", "CSE", 2, true, AccountRole.STUDENT);
	}
}

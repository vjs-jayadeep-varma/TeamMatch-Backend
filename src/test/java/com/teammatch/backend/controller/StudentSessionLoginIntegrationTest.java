package com.teammatch.backend.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.teammatch.backend.config.DevSecurityConfiguration;
import com.teammatch.backend.config.PasswordEncoderConfiguration;
import com.teammatch.backend.config.SecurityConfiguration;
import com.teammatch.backend.dto.StudentResponse;
import com.teammatch.backend.entity.Student;
import com.teammatch.backend.entity.enums.AccountRole;
import com.teammatch.backend.repository.StudentRepository;
import com.teammatch.backend.service.StudentService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(AuthenticationController.class)
@Import({ DevSecurityConfiguration.class, PasswordEncoderConfiguration.class, SecurityConfiguration.class })
@ActiveProfiles("dev")
@TestPropertySource(properties = {
		"TEAMMATCH_DEV_USERNAME=shared-dev@example.com",
		"TEAMMATCH_DEV_PASSWORD=test-only-development-password",
		"TEAMMATCH_ADMIN_USERNAME=admin@example.com",
		"TEAMMATCH_ADMIN_PASSWORD=test-only-admin-password"
})
class StudentSessionLoginIntegrationTest {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@MockitoBean
	private StudentRepository studentRepository;

	@MockitoBean
	private StudentService studentService;

	@Test
	void validStudentCredentialsCreateAnAuthenticatedSession() throws Exception {
		Student student = student(15L, "student@example.com", passwordEncoder.encode("student-password-123"));
		when(studentRepository.findByEmail("student@example.com")).thenReturn(Optional.of(student));
		when(studentService.findById(15L)).thenReturn(studentResponse());

		MvcResult result = mockMvc.perform(post("/api/auth/login")
					.with(csrf())
					.contentType("application/json")
					.content("{\"email\":\"student@example.com\",\"password\":\"student-password-123\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(15))
				.andReturn();

		MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(15));
	}

	@Test
	void invalidStudentPasswordIsRejected() throws Exception {
		Student student = student(15L, "student@example.com", passwordEncoder.encode("student-password-123"));
		when(studentRepository.findByEmail("student@example.com")).thenReturn(Optional.of(student));

		mockMvc.perform(post("/api/auth/login")
					.with(csrf())
					.contentType("application/json")
					.content("{\"email\":\"student@example.com\",\"password\":\"incorrect-password\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void sharedDevelopmentAccountIsNotAcceptedByStudentLogin() throws Exception {
		mockMvc.perform(post("/api/auth/login")
					.with(csrf())
					.contentType("application/json")
					.content("{\"email\":\"shared-dev@example.com\",\"password\":\"test-only-development-password\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void configuredAdminCredentialsCreateAnAdminSession() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/login")
					.with(csrf())
					.contentType("application/json")
					.content("{\"email\":\"ADMIN@example.com\",\"password\":\"test-only-admin-password\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").doesNotExist())
				.andExpect(jsonPath("$.accountRole").value("ADMIN"))
				.andExpect(jsonPath("$.email").value("admin@example.com"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andReturn();

		MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accountRole").value("ADMIN"));
	}

	private Student student(Long id, String email, String passwordHash) {
		Student student = mock(Student.class);
		when(student.getId()).thenReturn(id);
		when(student.getEmail()).thenReturn(email);
		when(student.getPasswordHash()).thenReturn(passwordHash);
		when(student.getAccountRole()).thenReturn(AccountRole.STUDENT);
		return student;
	}

	private StudentResponse studentResponse() {
		return new StudentResponse(15L, "Student", "ROLL015", "student@example.com", "CSE", 2,
				true, AccountRole.STUDENT);
	}
}

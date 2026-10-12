package com.teammatch.backend.controller;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.teammatch.backend.config.DevSecurityConfiguration;
import com.teammatch.backend.config.PasswordEncoderConfiguration;
import com.teammatch.backend.dto.StudentSkillAssignmentRequest;
import com.teammatch.backend.dto.StudentSkillNameAssignmentRequest;
import com.teammatch.backend.dto.StudentSkillResponse;
import com.teammatch.backend.entity.enums.AccountRole;
import com.teammatch.backend.entity.enums.SkillProficiency;
import com.teammatch.backend.exception.DuplicateResourceException;
import com.teammatch.backend.exception.ResourceNotFoundException;
import com.teammatch.backend.repository.StudentRepository;
import com.teammatch.backend.security.TeamMatchPrincipal;
import com.teammatch.backend.service.StudentSkillService;
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

@WebMvcTest(StudentSkillController.class)
@Import({ DevSecurityConfiguration.class, PasswordEncoderConfiguration.class })
@ActiveProfiles("dev")
@TestPropertySource(properties = {
		"TEAMMATCH_DEV_USERNAME=test-user",
		"TEAMMATCH_DEV_PASSWORD=test-only-development-password"
})
class StudentSkillControllerTest {
	private static final String TEST_USERNAME = "test-user";
	private static final String TEST_PASSWORD = "test-only-development-password";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private StudentSkillService studentSkillService;

	@MockitoBean
	private StudentRepository studentRepository;

	@Test
	void assignReturnsCreatedWithResponseDto() throws Exception {
		when(studentSkillService.assign(1L, new StudentSkillAssignmentRequest(2L, SkillProficiency.ADVANCED)))
				.thenReturn(new StudentSkillResponse(1L, 2L, "Java", SkillProficiency.ADVANCED));

		mockMvc.perform(post("/api/students/1/skills")
					.with(user(principal(1L)))
					.with(csrf())
					.contentType("application/json")
					.content("{\"skillId\":2,\"proficiencyLevel\":\"ADVANCED\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.studentId").value(1))
				.andExpect(jsonPath("$.skillId").value(2))
				.andExpect(jsonPath("$.skillName").value("Java"))
				.andExpect(jsonPath("$.proficiencyLevel").value("ADVANCED"));
	}

	@Test
	void assignByNameReturnsCreatedWithResponseDto() throws Exception {
		when(studentSkillService.assignByName(1L,
				new StudentSkillNameAssignmentRequest("JavaScript", SkillProficiency.INTERMEDIATE)))
				.thenReturn(new StudentSkillResponse(1L, 5L, "JavaScript", SkillProficiency.INTERMEDIATE));

		mockMvc.perform(post("/api/students/1/skills/by-name")
					.with(user(principal(1L)))
					.with(csrf())
					.contentType("application/json")
					.content("{\"skillName\":\"JavaScript\",\"proficiencyLevel\":\"INTERMEDIATE\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.studentId").value(1))
				.andExpect(jsonPath("$.skillId").value(5))
				.andExpect(jsonPath("$.skillName").value("JavaScript"))
				.andExpect(jsonPath("$.proficiencyLevel").value("INTERMEDIATE"));
	}

	@Test
	void assignByNameRejectsBlankSkillName() throws Exception {
		mockMvc.perform(post("/api/students/1/skills/by-name")
					.with(user(principal(1L)))
					.with(csrf())
					.contentType("application/json")
					.content("{\"skillName\":\"   \"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void listReturnsStudentSkillDtos() throws Exception {
		when(studentSkillService.findByStudentId(1L))
				.thenReturn(List.of(new StudentSkillResponse(1L, 2L, "Java", SkillProficiency.INTERMEDIATE)));

		mockMvc.perform(get("/api/students/1/skills")
					.with(user(principal(1L))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].skillId").value(2))
				.andExpect(jsonPath("$[0].skillName").value("Java"));
	}

	@Test
	void missingStudentOrSkillReturnsNotFound() throws Exception {
		when(studentSkillService.assign(99L, new StudentSkillAssignmentRequest(2L, null)))
				.thenThrow(new ResourceNotFoundException("Student 99 was not found."));
		when(studentSkillService.assign(1L, new StudentSkillAssignmentRequest(99L, null)))
				.thenThrow(new ResourceNotFoundException("Skill 99 was not found."));

		mockMvc.perform(post("/api/students/99/skills")
					.with(user(principal(99L)))
					.with(csrf())
					.contentType("application/json")
					.content("{\"skillId\":2}"))
				.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/students/1/skills")
					.with(user(principal(1L)))
					.with(csrf())
					.contentType("application/json")
					.content("{\"skillId\":99}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void duplicateAssignmentReturnsConflict() throws Exception {
		when(studentSkillService.assign(1L, new StudentSkillAssignmentRequest(2L, null)))
				.thenThrow(new DuplicateResourceException("This skill is already assigned to the student."));

		mockMvc.perform(post("/api/students/1/skills")
					.with(user(principal(1L)))
					.with(csrf())
					.contentType("application/json")
					.content("{\"skillId\":2}"))
				.andExpect(status().isConflict());
	}

	@Test
	void missingSkillIdReturnsBadRequest() throws Exception {
		mockMvc.perform(post("/api/students/1/skills")
					.with(user(principal(1L)))
					.with(csrf())
					.contentType("application/json")
					.content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void rejectsCrossStudentSkillReadsAndAssignments() throws Exception {
		mockMvc.perform(get("/api/students/2/skills").with(user(principal(1L))))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/students/2/skills")
					.with(user(principal(1L)))
					.with(csrf())
					.contentType("application/json")
					.content("{\"skillId\":2}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/students/2/skills/by-name")
					.with(user(principal(1L)))
					.with(csrf())
					.contentType("application/json")
					.content("{\"skillName\":\"Java\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void sharedDevelopmentUserCannotManageStudentSkills() throws Exception {
		mockMvc.perform(get("/api/students/1/skills")
					.with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/students/1/skills/by-name")
					.with(httpBasic(TEST_USERNAME, TEST_PASSWORD))
					.with(csrf())
					.contentType("application/json")
					.content("{\"skillName\":\"Java\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void studentSkillEndpointsRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/students/1/skills"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/students/1/skills")
					.with(csrf())
					.contentType("application/json")
					.content("{\"skillId\":2}"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/students/1/skills/by-name")
					.with(csrf())
					.contentType("application/json")
					.content("{\"skillName\":\"Java\"}"))
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
}
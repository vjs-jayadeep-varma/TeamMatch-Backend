package com.teammatch.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.teammatch.backend.config.DevSecurityConfiguration;
import com.teammatch.backend.config.PasswordEncoderConfiguration;
import com.teammatch.backend.dto.ProjectCreateRequest;
import com.teammatch.backend.dto.ProjectResponse;
import com.teammatch.backend.dto.SkillResponse;
import com.teammatch.backend.entity.enums.AccountRole;
import com.teammatch.backend.entity.enums.ProjectStatus;
import com.teammatch.backend.exception.ResourceNotFoundException;
import com.teammatch.backend.exception.ForbiddenOperationException;
import com.teammatch.backend.security.TeamMatchPrincipal;
import com.teammatch.backend.service.ProjectService;
import com.teammatch.backend.repository.StudentRepository;
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

@WebMvcTest(ProjectController.class)
@Import({ DevSecurityConfiguration.class, PasswordEncoderConfiguration.class })
@ActiveProfiles("dev")
@TestPropertySource(properties = {
		"TEAMMATCH_DEV_USERNAME=test-user",
		"TEAMMATCH_DEV_PASSWORD=test-only-development-password"
})
class ProjectControllerTest {
	private static final String TEST_USERNAME = "test-user";
	private static final String TEST_PASSWORD = "test-only-development-password";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProjectService projectService;

	@MockitoBean
	private StudentRepository studentRepository;

	@Test
	void createReturnsCreatedProjectDto() throws Exception {
		when(projectService.create(any(ProjectCreateRequest.class))).thenReturn(projectResponse());

		mockMvc.perform(post("/api/projects")
					.with(user(principal(1L)))
					.with(csrf())
					.contentType("application/json")
					.content("{\"title\":\"Project1\",\"description\":\"Demo project\",\"ownerStudentId\":1,\"requiredTeamSize\":4,\"requiredSkillIds\":[2]}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(5))
				.andExpect(jsonPath("$.status").value("OPEN"))
				.andExpect(jsonPath("$.requiredSkills[0].name").value("Java"));
	}

	@Test
	void anotherStudentCanCreateAProjectForTheirOwnAccount() throws Exception {
		when(projectService.create(new ProjectCreateRequest(
				"Project2", "Shared project", 2L, 3, List.of())))
				.thenReturn(new ProjectResponse(6L, "Project2", "Shared project", 2L, 3,
						ProjectStatus.OPEN, List.of()));

		mockMvc.perform(post("/api/projects")
					.with(user(principal(2L)))
					.with(csrf())
					.contentType("application/json")
					.content("{\"title\":\"Project2\",\"description\":\"Shared project\",\"ownerStudentId\":2,\"requiredTeamSize\":3,\"requiredSkillIds\":[]}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.ownerStudentId").value(2));
	}

	@Test
	void listAndDetailReturnProjectDtos() throws Exception {
		when(projectService.findAll()).thenReturn(List.of(projectResponse(),
				new ProjectResponse(6L, "Shared project", "Shared description", 2L, 3,
						ProjectStatus.OPEN, List.of())));
		when(projectService.findById(5L)).thenReturn(projectResponse());

		mockMvc.perform(get("/api/projects").with(user(principal(3L))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(5))
				.andExpect(jsonPath("$[1].ownerStudentId").value(2));
		mockMvc.perform(get("/api/projects/5").with(user(principal(3L))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Project1"));
	}

	@Test
	void projectOwnerCanUpdateTheirProject() throws Exception {
		when(projectService.update(5L, 1L, new ProjectCreateRequest(
				"Updated project", "Updated description", 1L, 4, List.of(2L))))
				.thenReturn(new ProjectResponse(5L, "Updated project", "Updated description",
						1L, 4, ProjectStatus.OPEN, List.of(new SkillResponse(2L, "Java"))));

		mockMvc.perform(put("/api/projects/5")
					.with(user(principal(1L)))
					.with(csrf())
					.contentType("application/json")
					.content("{\"title\":\"Updated project\",\"description\":\"Updated description\",\"ownerStudentId\":1,\"requiredTeamSize\":4,\"requiredSkillIds\":[2]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Updated project"));
	}

	@Test
	void projectOwnerCanCloseOpenProject() throws Exception {
		when(projectService.close(5L, 1L)).thenReturn(new ProjectResponse(
				5L, "Project1", "Demo project", 1L, "Owner", 4, ProjectStatus.CANCELLED,
				List.of(), "Project1 Team", "Owner", List.of()));

		mockMvc.perform(patch("/api/projects/5/close")
					.with(user(principal(1L)))
					.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"));
	}

	@Test
	void nonOwnerCannotCloseProject() throws Exception {
		when(projectService.close(5L, 2L))
				.thenThrow(new ForbiddenOperationException("Only the project owner can close this project."));

		mockMvc.perform(patch("/api/projects/5/close")
					.with(user(principal(2L)))
					.with(csrf()))
				.andExpect(status().isForbidden());
	}

	@Test
	void closeRejectsNonStudentPrincipals() throws Exception {
		mockMvc.perform(patch("/api/projects/5/close")
					.with(user("admin").roles("ADMIN"))
					.with(csrf()))
				.andExpect(status().isForbidden());
	}

	@Test
	void closingProjectWithInvalidStatusReturnsConflict() throws Exception {
		when(projectService.close(5L, 1L))
				.thenThrow(new com.teammatch.backend.exception.DuplicateResourceException(
						"Only an open project can be cancelled."));

		mockMvc.perform(patch("/api/projects/5/close")
					.with(user(principal(1L)))
					.with(csrf()))
				.andExpect(status().isConflict());
	}

	@Test
	void updateRequiresStudentAuthentication() throws Exception {
		when(projectService.update(5L, 1L, new ProjectCreateRequest(
				"Updated project", "Updated description", 2L, 4, List.of())))
				.thenThrow(new ForbiddenOperationException("Only the project owner can edit this project."));

		mockMvc.perform(put("/api/projects/5")
					.with(user(principal(1L)))
					.with(csrf())
					.contentType("application/json")
					.content("{\"title\":\"Updated project\",\"description\":\"Updated description\",\"ownerStudentId\":2,\"requiredTeamSize\":4,\"requiredSkillIds\":[]}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void invalidTeamSizeReturnsBadRequest() throws Exception {
		mockMvc.perform(post("/api/projects")
					.with(user(principal(1L)))
					.with(csrf())
					.contentType("application/json")
					.content("{\"title\":\"Project1\",\"description\":\"Demo\",\"ownerStudentId\":1,\"requiredTeamSize\":1,\"requiredSkillIds\":[]}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void missingOwnerOrSkillReturnsNotFound() throws Exception {
		when(projectService.create(new ProjectCreateRequest("Project1", "Demo", 99L, 4, List.of())))
				.thenThrow(new ResourceNotFoundException("Student 99 was not found."));
		when(projectService.create(new ProjectCreateRequest("Project1", "Demo", 1L, 4, List.of(99L))))
				.thenThrow(new ResourceNotFoundException("Skill 99 was not found."));

		mockMvc.perform(post("/api/projects")
					.with(user(principal(99L)))
					.with(csrf())
					.contentType("application/json")
					.content("{\"title\":\"Project1\",\"description\":\"Demo\",\"ownerStudentId\":99,\"requiredTeamSize\":4,\"requiredSkillIds\":[]}"))
				.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/projects")
					.with(user(principal(1L)))
					.with(csrf())
					.contentType("application/json")
					.content("{\"title\":\"Project1\",\"description\":\"Demo\",\"ownerStudentId\":1,\"requiredTeamSize\":4,\"requiredSkillIds\":[99]}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void sharedDevelopmentUserCannotCreateAStudentProject() throws Exception {
		mockMvc.perform(post("/api/projects")
					.with(httpBasic(TEST_USERNAME, TEST_PASSWORD))
					.with(csrf())
					.contentType("application/json")
					.content("{\"title\":\"Project1\",\"description\":\"Demo project\",\"ownerStudentId\":1,\"requiredTeamSize\":4,\"requiredSkillIds\":[]}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void studentCannotCreateProjectForAnotherStudent() throws Exception {
		mockMvc.perform(post("/api/projects")
					.with(user(principal(1L)))
					.with(csrf())
					.contentType("application/json")
					.content("{\"title\":\"Project1\",\"description\":\"Demo project\",\"ownerStudentId\":2,\"requiredTeamSize\":4,\"requiredSkillIds\":[]}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void projectCreationRequiresAuthentication() throws Exception {
		mockMvc.perform(post("/api/projects")
					.with(csrf())
					.contentType("application/json")
					.content("{\"title\":\"Project1\",\"description\":\"Demo project\",\"ownerStudentId\":1,\"requiredTeamSize\":4,\"requiredSkillIds\":[]}"))
				.andExpect(status().isUnauthorized());
	}

	private TeamMatchPrincipal principal(Long studentId) {
		TeamMatchPrincipal principal = mock(TeamMatchPrincipal.class);
		when(principal.getStudentId()).thenReturn(studentId);
		when(principal.getRole()).thenReturn(AccountRole.STUDENT);
		doReturn(List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))).when(principal).getAuthorities();
		return principal;
	}

	private ProjectResponse projectResponse() {
		return new ProjectResponse(5L, "Project1", "Demo project", 1L, 4, ProjectStatus.OPEN,
				List.of(new SkillResponse(2L, "Java")));
	}
}
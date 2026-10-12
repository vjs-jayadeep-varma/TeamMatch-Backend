package com.teammatch.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.teammatch.backend.config.SecurityConfiguration;
import com.teammatch.backend.dto.SkillRequest;
import com.teammatch.backend.dto.SkillResponse;
import com.teammatch.backend.service.SkillService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SkillController.class)
@Import(SecurityConfiguration.class)
class SkillControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SkillService skillService;

	@Test
	void studentsCanReadSharedSkillCatalog() throws Exception {
		when(skillService.findAll()).thenReturn(List.of(new SkillResponse(1L, "Java")));

		mockMvc.perform(get("/api/skills").with(user("student").roles("STUDENT")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Java"));
	}

	@Test
	void unauthenticatedUserCannotReadSkillCatalog() throws Exception {
		mockMvc.perform(get("/api/skills")).andExpect(status().isUnauthorized());
		verifyNoInteractions(skillService);
	}

	@Test
	void studentCannotCreateOrRenameSharedSkills() throws Exception {
		mockMvc.perform(post("/api/skills")
					.with(user("student").roles("STUDENT"))
					.with(csrf())
					.contentType("application/json")
					.content("{\"name\":\"Java\"}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(put("/api/skills/1")
					.with(user("student").roles("STUDENT"))
					.with(csrf())
					.contentType("application/json")
					.content("{\"name\":\"Java\"}"))
				.andExpect(status().isForbidden());
		verifyNoInteractions(skillService);
	}

	@Test
	void administratorCanCreateAndRenameSharedSkills() throws Exception {
		when(skillService.create(any(SkillRequest.class))).thenReturn(new SkillResponse(1L, "Java"));
		when(skillService.update(eq(1L), any(SkillRequest.class))).thenReturn(new SkillResponse(1L, "Java"));

		mockMvc.perform(post("/api/skills")
					.with(user("admin").roles("ADMIN"))
					.with(csrf())
					.contentType("application/json")
					.content("{\"name\":\"Java\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Java"));
		mockMvc.perform(put("/api/skills/1")
					.with(user("admin").roles("ADMIN"))
					.with(csrf())
					.contentType("application/json")
					.content("{\"name\":\"Java\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Java"));
	}

	@Test
	void unauthenticatedUserCannotMutateSharedSkills() throws Exception {
		mockMvc.perform(post("/api/skills")
					.with(csrf())
					.contentType("application/json")
					.content("{\"name\":\"Java\"}"))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(skillService);
	}
}

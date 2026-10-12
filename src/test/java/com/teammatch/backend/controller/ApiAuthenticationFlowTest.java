package com.teammatch.backend.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.clearInvocations;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.teammatch.backend.config.DevSecurityConfiguration;
import com.teammatch.backend.config.PasswordEncoderConfiguration;
import com.teammatch.backend.config.SecurityConfiguration;
import com.teammatch.backend.service.SkillService;
import com.teammatch.backend.service.StudentService;
import com.teammatch.backend.repository.StudentRepository;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = { CsrfController.class, SkillController.class, StudentController.class })
@Import({ DevSecurityConfiguration.class, PasswordEncoderConfiguration.class, SecurityConfiguration.class })
@ActiveProfiles("dev")
@TestPropertySource(properties = {
		"TEAMMATCH_DEV_USERNAME=test-user",
		"TEAMMATCH_DEV_PASSWORD=test-only-development-password"
})
class ApiAuthenticationFlowTest {
	private static final String TEST_USERNAME = "test-user";
	private static final String TEST_PASSWORD = "test-only-development-password";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SkillService skillService;

	@MockitoBean
	private StudentService studentService;

	@MockitoBean
	private StudentRepository studentRepository;

	@Test
	void authenticatedCsrfTokenCanBeUsedForPost() throws Exception {
		MvcResult tokenResult = mockMvc.perform(get("/api/csrf")
					.with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
				.andExpect(status().isOk())
				.andReturn();
		String tokenResponse = tokenResult.getResponse().getContentAsString();
		String token = jsonString(tokenResponse, "token");
		String headerName = jsonString(tokenResponse, "headerName");
		MockHttpSession session = (MockHttpSession) tokenResult.getRequest().getSession(false);
		assertNotNull(session);

		mockMvc.perform(post("/api/students")
					.session(session)
					.with(httpBasic(TEST_USERNAME, TEST_PASSWORD))
					.header(headerName, token)
					.contentType("application/json")
					.content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void basicAuthenticationCanReadSkillCatalogButCannotMutateIt() throws Exception {
		mockMvc.perform(get("/api/skills")
					.with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
				.andExpect(status().isOk());
		clearInvocations(skillService);

		mockMvc.perform(post("/api/skills")
					.with(httpBasic(TEST_USERNAME, TEST_PASSWORD))
					.contentType("application/json")
					.content("{}"))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/skills")
					.with(httpBasic(TEST_USERNAME, TEST_PASSWORD))
					.with(csrf())
					.contentType("application/json")
					.content("{}"))
				.andExpect(status().isForbidden());

		verifyNoInteractions(skillService);
	}

	@Test
	void studentListIsAdminOnlyAndRegistrationReachesValidation() throws Exception {
		mockMvc.perform(get("/api/students")
					.with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
				.andExpect(status().isForbidden());
		clearInvocations(studentService);

		mockMvc.perform(post("/api/students")
					.with(httpBasic(TEST_USERNAME, TEST_PASSWORD))
					.contentType("application/json")
					.content("{}"))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/students")
					.with(httpBasic(TEST_USERNAME, TEST_PASSWORD))
					.with(csrf())
					.contentType("application/json")
					.content("{}"))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(studentService);
	}

	private static String jsonString(String json, String property) {
		Matcher matcher = Pattern.compile("\"" + Pattern.quote(property) + "\":\"([^\"]+)\"").matcher(json);
		if (!matcher.find()) {
			throw new AssertionError("Missing JSON string property: " + property);
		}
		return matcher.group(1);
	}
}
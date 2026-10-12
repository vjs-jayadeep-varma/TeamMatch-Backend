package com.teammatch.backend.controller;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.teammatch.backend.config.SecurityConfiguration;
import com.teammatch.backend.dto.StudentRecommendationResponse;
import com.teammatch.backend.entity.enums.AccountRole;
import com.teammatch.backend.exception.ApiExceptionHandler;
import com.teammatch.backend.exception.DuplicateResourceException;
import com.teammatch.backend.exception.ResourceNotFoundException;
import com.teammatch.backend.security.TeamMatchPrincipal;
import com.teammatch.backend.service.MatchingService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MatchingController.class)
@Import({ SecurityConfiguration.class, ApiExceptionHandler.class })
class MatchingControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MatchingService matchingService;

	@Test
	void unauthenticatedUserCannotRequestRecommendations() throws Exception {
		mockMvc.perform(get("/api/projects/10/recommendations"))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(matchingService);
	}

	@Test
	void nonStudentPrincipalCannotRequestRecommendations() throws Exception {
		mockMvc.perform(get("/api/projects/10/recommendations").with(user("admin").roles("ADMIN")))
				.andExpect(status().isForbidden());
		verifyNoInteractions(matchingService);
	}

	@Test
	void authenticatedStudentRequestsRecommendationsForSharedProject() throws Exception {
		when(matchingService.recommendForProject(10L, 5L)).thenReturn(List.of(
				new StudentRecommendationResponse(6L, "Alex", "alex@example.com", "CSE", 2,
						75.0, List.of("Java"), List.of("React"))));

		mockMvc.perform(get("/api/projects/10/recommendations").with(user(principal(5L))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].studentId").value(6))
				.andExpect(jsonPath("$[0].compatibilityScore").value(75.0));
	}

	@Test
	void validRecommendationRequestReturnsSuccessfulEmptyListWhenThereAreNoCandidates() throws Exception {
		when(matchingService.recommendForProject(10L, 5L)).thenReturn(List.of());

		mockMvc.perform(get("/api/projects/10/recommendations").with(user(principal(5L))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isEmpty());
	}

	@Test
	void missingProjectReturnsNotFoundForValidRecommendationRoute() throws Exception {
		when(matchingService.recommendForProject(99L, 5L))
				.thenThrow(new ResourceNotFoundException("Project 99 was not found."));

		mockMvc.perform(get("/api/projects/99/recommendations").with(user(principal(5L))))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Project 99 was not found."));
	}

	@Test
	void malformedProjectIdReturnsActionableBadRequest() throws Exception {
		mockMvc.perform(get("/api/projects/not-a-number/recommendations").with(user(principal(5L))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(
						"Path or query parameter 'projectId' has an invalid value."));
		verifyNoInteractions(matchingService);
	}

	@Test
	void ineligibleProjectReturnsConflictInsteadOfAnEmptySuccess() throws Exception {
		when(matchingService.recommendForProject(10L, 5L))
				.thenThrow(new DuplicateResourceException("Recommendations are not available for this project."));

		mockMvc.perform(get("/api/projects/10/recommendations").with(user(principal(5L))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Recommendations are not available for this project."));
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

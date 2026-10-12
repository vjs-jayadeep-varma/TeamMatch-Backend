package com.teammatch.backend.controller;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.teammatch.backend.config.DevSecurityConfiguration;
import com.teammatch.backend.config.PasswordEncoderConfiguration;
import com.teammatch.backend.dto.TeamRequestCreateRequest;
import com.teammatch.backend.dto.TeamRequestDecisionRequest;
import com.teammatch.backend.dto.TeamRequestResponse;
import com.teammatch.backend.entity.enums.AccountRole;
import com.teammatch.backend.entity.enums.TeamRequestStatus;
import com.teammatch.backend.exception.ForbiddenOperationException;
import com.teammatch.backend.service.TeamRequestService;
import com.teammatch.backend.security.TeamMatchPrincipal;
import com.teammatch.backend.repository.StudentRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TeamRequestController.class)
@Import({ DevSecurityConfiguration.class, PasswordEncoderConfiguration.class })
@ActiveProfiles("dev")
@TestPropertySource(properties = {
		"TEAMMATCH_DEV_USERNAME=test-user",
		"TEAMMATCH_DEV_PASSWORD=test-only-development-password"
})
class TeamRequestControllerTest {
	private static final String TEST_USERNAME = "test-user";
	private static final String TEST_PASSWORD = "test-only-development-password";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TeamRequestService teamRequestService;

	@MockitoBean
	private StudentRepository studentRepository;

	@Test
	void createRequiresAuthenticationAndReturnsCreatedRequest() throws Exception {
		when(teamRequestService.create(1L, 10L, new TeamRequestCreateRequest(2L))).thenReturn(response());

		mockMvc.perform(post("/api/projects/10/team-requests")
					.with(user(principal(1L)))
					.with(csrf())
					.contentType("application/json")
					.content("{\"recipientStudentId\":2}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(30))
				.andExpect(jsonPath("$.recipientStudentId").value(2))
				.andExpect(jsonPath("$.status").value("PENDING"));
	}

	@Test
	void createValidatesBothStudentIds() throws Exception {
		mockMvc.perform(post("/api/projects/10/team-requests")
					.with(user(principal(1L)))
					.with(csrf())
					.contentType("application/json")
					.content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void incomingAndOutgoingListsUseStudentScopedEndpoints() throws Exception {
		when(teamRequestService.findIncoming(2L, 2L)).thenReturn(List.of(response()));
		when(teamRequestService.findOutgoing(1L, 1L)).thenReturn(List.of(response()));

		mockMvc.perform(get("/api/students/2/team-requests/incoming")
					.with(user(principal(2L))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].requestingStudentId").value(1));
		mockMvc.perform(get("/api/students/1/team-requests/outgoing")
					.with(user(principal(1L))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].recipientStudentId").value(2));
	}

	@Test
	void incomingHistoryUsesAuthenticatedRecipientScopedEndpoint() throws Exception {
		when(teamRequestService.findIncomingHistory(2L, 2L)).thenReturn(List.of(
				new TeamRequestResponse(31L, 10L, "Project", 1L, "Owner", 2L, "Invitee",
						TeamRequestStatus.REJECTED, Instant.parse("2026-01-01T00:00:00Z"),
						Instant.parse("2026-01-02T00:00:00Z"))));

		mockMvc.perform(get("/api/students/2/team-requests/incoming/history")
					.with(user(principal(2L))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].status").value("REJECTED"))
				.andExpect(jsonPath("$[0].recipientStudentId").value(2));
	}

	@Test
	void incomingHistoryCannotBeReadForAnotherStudent() throws Exception {
		when(teamRequestService.findIncomingHistory(2L, 3L))
				.thenThrow(new ForbiddenOperationException("You can only access your own team requests."));

		mockMvc.perform(get("/api/students/3/team-requests/incoming/history")
					.with(user(principal(2L))))
				.andExpect(status().isForbidden());
	}

	@Test
	void responseRejectsWrongRecipient() throws Exception {
		when(teamRequestService.respond(3L, 3L, 30L, new TeamRequestDecisionRequest(TeamRequestStatus.ACCEPTED)))
				.thenThrow(new ForbiddenOperationException("Only the invited student can respond to this request."));

		mockMvc.perform(patch("/api/students/3/team-requests/30/response")
					.with(user(principal(3L)))
					.with(csrf())
					.contentType("application/json")
					.content("{\"status\":\"ACCEPTED\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Only the invited student can respond to this request."));
	}

	@Test
	void projectOwnerCanRespondToAJoinRequestAddressedToThem() throws Exception {
		when(teamRequestService.respond(1L, 1L, 30L, new TeamRequestDecisionRequest(TeamRequestStatus.ACCEPTED)))
				.thenReturn(new TeamRequestResponse(30L, 10L, "Project", 2L, "Applicant", 1L, "Owner",
						TeamRequestStatus.ACCEPTED, Instant.parse("2026-01-01T00:00:00Z"),
						Instant.parse("2026-01-01T00:00:00Z")));

		mockMvc.perform(patch("/api/students/1/team-requests/30/response")
					.with(user(principal(1L)))
					.with(csrf())
					.contentType("application/json")
					.content("{\"status\":\"ACCEPTED\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.requestingStudentId").value(2))
				.andExpect(jsonPath("$.recipientStudentId").value(1))
				.andExpect(jsonPath("$.status").value("ACCEPTED"));
	}

	@Test
	void stateChangingRequestRequiresCsrfToken() throws Exception {
		mockMvc.perform(post("/api/projects/10/team-requests")
					.with(httpBasic(TEST_USERNAME, TEST_PASSWORD))
					.contentType("application/json")
					.content("{\"recipientStudentId\":2}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void sharedDevelopmentUserCannotActAsStudent() throws Exception {
		mockMvc.perform(get("/api/students/2/team-requests/incoming")
					.with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
				.andExpect(status().isForbidden());
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

	private TeamRequestResponse response() {
		Instant now = Instant.parse("2026-01-01T00:00:00Z");
		return new TeamRequestResponse(30L, 10L, "Project", 1L, "Owner", 2L, "Invitee",
				TeamRequestStatus.PENDING, now, now);
	}
}

package com.teammatch.backend.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.teammatch.backend.config.SecurityConfiguration;
import com.teammatch.backend.dto.AdminMonitoringResponse;
import com.teammatch.backend.dto.AdminPage;
import com.teammatch.backend.dto.AdminProjectDetail;
import com.teammatch.backend.dto.AdminProjectSummary;
import com.teammatch.backend.dto.AdminStudentDetail;
import com.teammatch.backend.dto.AdminStudentListItem;
import com.teammatch.backend.dto.AdminStudentSummary;
import com.teammatch.backend.dto.AdminTeamRequestDetail;
import com.teammatch.backend.dto.AdminTeamMemberSummary;
import com.teammatch.backend.dto.AdminTeamRequestSummary;
import com.teammatch.backend.dto.AdminTeamSummary;
import com.teammatch.backend.entity.enums.AccountRole;
import com.teammatch.backend.entity.enums.MembershipStatus;
import com.teammatch.backend.entity.enums.ProjectStatus;
import com.teammatch.backend.entity.enums.TeamMemberRole;
import com.teammatch.backend.entity.enums.TeamRequestStatus;
import com.teammatch.backend.entity.enums.TeamStatus;
import com.teammatch.backend.service.AdminMonitoringService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminMonitoringController.class)
@Import(SecurityConfiguration.class)
class AdminMonitoringControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AdminMonitoringService adminMonitoringService;

	@Test
	void unauthenticatedUserCannotAccessMonitoringData() throws Exception {
		mockMvc.perform(get("/api/admin/monitoring"))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(adminMonitoringService);
	}

	@Test
	void studentCannotAccessMonitoringData() throws Exception {
		mockMvc.perform(get("/api/admin/monitoring").with(user("student").roles("STUDENT")))
				.andExpect(status().isForbidden());
		verifyNoInteractions(adminMonitoringService);
	}

	@Test
	void studentCannotSearchAdministratorStudentRecords() throws Exception {
		mockMvc.perform(get("/api/admin/students").with(user("student").roles("STUDENT")))
				.andExpect(status().isForbidden());
		verifyNoInteractions(adminMonitoringService);
	}

	@Test
	void administratorCanSearchPaginatedStudentRecords() throws Exception {
		when(adminMonitoringService.findStudents("alex", 0, 20)).thenReturn(
				new AdminPage<>(List.of(new AdminStudentListItem(
						4L, "Alex Student", "R004", "alex@example.com", "CSE", 2)), 0, 20, 1, 1));

		mockMvc.perform(get("/api/admin/students").param("query", "alex")
					.with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].rollNumber").value("R004"))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
	}

	@Test
	void administratorCanReviewStudentAndProjectDetails() throws Exception {
		when(adminMonitoringService.findStudent(4L)).thenReturn(new AdminStudentDetail(
				4L, "Alex Student", "R004", "alex@example.com", "CSE", 2, true,
				List.of("Java"), List.of("Robotics"), List.of(), List.of()));
		when(adminMonitoringService.findProject(8L)).thenReturn(new AdminProjectDetail(
				8L, "Matching app", "A shared project", ProjectStatus.IN_PROGRESS, 4,
				4L, "Alex Student", List.of("Java"), 9L, "Matching team", TeamStatus.FORMING,
				List.of(new AdminTeamMemberSummary(4L, "Alex Student", TeamMemberRole.LEADER,
						MembershipStatus.ACTIVE))));

		mockMvc.perform(get("/api/admin/students/4").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rollNumber").value("R004"))
				.andExpect(jsonPath("$.skills[0]").value("Java"))
				.andExpect(jsonPath("$.interests[0]").value("Robotics"))
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
		mockMvc.perform(get("/api/admin/projects/8").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.description").value("A shared project"))
				.andExpect(jsonPath("$.requiredSkills[0]").value("Java"))
				.andExpect(jsonPath("$.members[0].studentName").value("Alex Student"));
	}

	@Test
	void studentCannotReviewAdministratorProjectDetails() throws Exception {
		mockMvc.perform(get("/api/admin/projects/8").with(user("student").roles("STUDENT")))
				.andExpect(status().isForbidden());
		verifyNoInteractions(adminMonitoringService);
	}

	@Test
	void administratorCanReviewFilteredRequestsAndInvitationDirection() throws Exception {
		when(adminMonitoringService.findRequests(TeamRequestStatus.PENDING, 8L, 0, 20))
				.thenReturn(new AdminPage<>(List.of(new AdminTeamRequestDetail(
						12L, 8L, "Matching app", 9L, "Matching team", 4L, "Alex Student",
						"INVITATION", 5L, "Sam Student", TeamRequestStatus.PENDING,
						Instant.parse("2026-01-02T03:04:05Z"), Instant.parse("2026-01-02T03:04:05Z"))),
						0, 20, 1, 1));

		mockMvc.perform(get("/api/admin/team-requests")
					.param("status", "PENDING")
					.param("projectId", "8")
					.with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].requestType").value("INVITATION"))
				.andExpect(jsonPath("$.content[0].teamName").value("Matching team"))
				.andExpect(jsonPath("$.content[0].status").value("PENDING"));
	}

	@Test
	void adminReceivesMonitoringSummariesWithoutCredentialFields() throws Exception {
		Instant created = Instant.parse("2026-01-02T03:04:05Z");
		AdminMonitoringResponse response = new AdminMonitoringResponse(
				List.of(new AdminStudentSummary(4L, "Sam", "CSE", 2, AccountRole.STUDENT)),
				List.of(new AdminProjectSummary(8L, "Matching app", ProjectStatus.OPEN, 4, "Sam")),
				List.of(new AdminTeamSummary(9L, "Matching team", TeamStatus.FORMING, 8L, "Matching app", List.of())),
				List.of(new AdminTeamRequestSummary(
						12L, "Matching app", "Sam", "Alex", TeamRequestStatus.PENDING, created, created)));
		when(adminMonitoringService.getMonitoringData()).thenReturn(response);

		mockMvc.perform(get("/api/admin/monitoring").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.students[0].name").value("Sam"))
				.andExpect(jsonPath("$.students[0].accountRole").value("STUDENT"))
				.andExpect(jsonPath("$.projects[0].status").value("OPEN"))
				.andExpect(jsonPath("$.teams[0].members").isArray())
				.andExpect(jsonPath("$.teamRequests[0].status").value("PENDING"))
				.andExpect(jsonPath("$.students[0].password").doesNotExist())
				.andExpect(jsonPath("$.students[0].passwordHash").doesNotExist())
				.andExpect(jsonPath("$.students[0].email").doesNotExist());
	}
}

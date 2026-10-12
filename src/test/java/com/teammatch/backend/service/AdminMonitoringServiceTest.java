package com.teammatch.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import com.teammatch.backend.dto.AdminOverviewResponse;
import com.teammatch.backend.dto.AdminMonitoringResponse;
import com.teammatch.backend.dto.AdminProjectSummary;
import com.teammatch.backend.dto.AdminStudentSummary;
import com.teammatch.backend.dto.AdminTeamMemberRow;
import com.teammatch.backend.dto.AdminTeamRequestSummary;
import com.teammatch.backend.entity.enums.AccountRole;
import com.teammatch.backend.entity.enums.MembershipStatus;
import com.teammatch.backend.entity.enums.ProjectStatus;
import com.teammatch.backend.entity.enums.TeamMemberRole;
import com.teammatch.backend.entity.enums.TeamRequestStatus;
import com.teammatch.backend.entity.enums.TeamStatus;
import com.teammatch.backend.repository.ProjectRepository;
import com.teammatch.backend.repository.StudentRepository;
import com.teammatch.backend.repository.StudentSkillRepository;
import com.teammatch.backend.repository.TeamMembershipRepository;
import com.teammatch.backend.repository.TeamRepository;
import com.teammatch.backend.repository.TeamRequestRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminMonitoringServiceTest {
	@Mock
	private StudentRepository studentRepository;

	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private TeamRepository teamRepository;

	@Mock
	private TeamRequestRepository teamRequestRepository;

	@Mock
	private StudentSkillRepository studentSkillRepository;

	@Mock
	private TeamMembershipRepository teamMembershipRepository;

	@InjectMocks
	private AdminMonitoringService adminMonitoringService;

	@Test
	void combinesDatabaseSummariesAndGroupsMembershipsByTeam() {
		Instant created = Instant.parse("2026-01-02T03:04:05Z");
		when(studentRepository.findAllAdminSummaries()).thenReturn(
				List.of(new AdminStudentSummary(1L, "Sam", "CSE", 2, AccountRole.STUDENT)));
		when(projectRepository.findAllAdminSummaries()).thenReturn(
				List.of(new AdminProjectSummary(4L, "Build", ProjectStatus.OPEN, 4, "Sam")));
		when(teamRepository.findAllAdminSummaries()).thenReturn(List.of(
				new AdminTeamMemberRow(6L, "Team", TeamStatus.FORMING, 4L, "Build", 1L, "Sam",
						TeamMemberRole.LEADER, MembershipStatus.ACTIVE),
				new AdminTeamMemberRow(6L, "Team", TeamStatus.FORMING, 4L, "Build", 2L, "Alex",
						TeamMemberRole.MEMBER, MembershipStatus.LEFT),
				new AdminTeamMemberRow(7L, "Empty", TeamStatus.FORMING, 5L, "Another project", null, null,
						null, null)));
		when(teamRequestRepository.findAllAdminSummaries()).thenReturn(
				List.of(new AdminTeamRequestSummary(9L, "Build", "Sam", "Alex",
						TeamRequestStatus.REJECTED, created, created)));

		AdminMonitoringResponse response = adminMonitoringService.getMonitoringData();

		assertEquals("Sam", response.students().get(0).name());
		assertEquals(ProjectStatus.OPEN, response.projects().get(0).status());
		assertEquals(2, response.teams().get(0).members().size());
		assertEquals(MembershipStatus.LEFT, response.teams().get(0).members().get(1).status());
		assertEquals(0, response.teams().get(1).members().size());
		assertEquals(TeamRequestStatus.REJECTED, response.teamRequests().get(0).status());
	}

	@Test
	void overviewReportsPlatformTotalsAndRequestStatuses() {
		when(studentRepository.count()).thenReturn(12L);
		when(projectRepository.count()).thenReturn(8L);
		when(teamRepository.count()).thenReturn(6L);
		when(teamRequestRepository.count()).thenReturn(10L);
		when(teamRequestRepository.countByStatus(TeamRequestStatus.PENDING)).thenReturn(3L);
		when(teamRequestRepository.countByStatus(TeamRequestStatus.ACCEPTED)).thenReturn(5L);
		when(teamRequestRepository.countByStatus(TeamRequestStatus.REJECTED)).thenReturn(2L);

		AdminOverviewResponse response = adminMonitoringService.getOverview();

		assertEquals(12L, response.students());
		assertEquals(8L, response.projects());
		assertEquals(6L, response.teams());
		assertEquals(10L, response.requests());
		assertEquals(3L, response.requestsByStatus().get("PENDING"));
		assertEquals(5L, response.requestsByStatus().get("ACCEPTED"));
		assertEquals(2L, response.requestsByStatus().get("REJECTED"));
	}

	@Test
	void studentSearchLimitsPageSizeToOneHundred() {
		when(studentRepository.searchAdminStudents(org.mockito.ArgumentMatchers.eq("sam"), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of()));

		adminMonitoringService.findStudents("sam", 0, 500);

		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(studentRepository).searchAdminStudents(org.mockito.ArgumentMatchers.eq("sam"), pageable.capture());
		assertEquals(100, pageable.getValue().getPageSize());
	}
}

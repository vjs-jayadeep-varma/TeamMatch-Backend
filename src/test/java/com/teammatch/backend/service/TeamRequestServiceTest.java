package com.teammatch.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.teammatch.backend.dto.StudentRecommendationResponse;
import com.teammatch.backend.dto.TeamRequestCreateRequest;
import com.teammatch.backend.dto.TeamRequestDecisionRequest;
import com.teammatch.backend.dto.TeamRequestResponse;
import com.teammatch.backend.entity.Project;
import com.teammatch.backend.entity.Student;
import com.teammatch.backend.entity.Team;
import com.teammatch.backend.entity.TeamMembership;
import com.teammatch.backend.entity.TeamRequest;
import com.teammatch.backend.entity.enums.MembershipStatus;
import com.teammatch.backend.entity.enums.ProjectStatus;
import com.teammatch.backend.entity.enums.TeamMemberRole;
import com.teammatch.backend.entity.enums.TeamRequestStatus;
import com.teammatch.backend.entity.enums.TeamStatus;
import com.teammatch.backend.exception.DuplicateResourceException;
import com.teammatch.backend.exception.ForbiddenOperationException;
import com.teammatch.backend.repository.ProjectRepository;
import com.teammatch.backend.repository.StudentRepository;
import com.teammatch.backend.repository.TeamMembershipRepository;
import com.teammatch.backend.repository.TeamRepository;
import com.teammatch.backend.repository.TeamRequestRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamRequestServiceTest {
	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private StudentRepository studentRepository;

	@Mock
	private TeamRepository teamRepository;

	@Mock
	private TeamMembershipRepository teamMembershipRepository;

	@Mock
	private TeamRequestRepository teamRequestRepository;

	@Mock
	private MatchingService matchingService;

	@InjectMocks
	private TeamRequestService teamRequestService;

	@Test
	void createStoresInviteAndCreatesFormingTeamWithOwner() {
		Student owner = student(1L, "Owner");
		Student recipient = student(2L, "Recipient");
		Project project = project(owner, 4);
		Team team = mock(Team.class);
		when(team.getId()).thenReturn(20L);
		when(team.getProject()).thenReturn(project);
		when(team.getTeamName()).thenReturn("Project Team");
		when(projectRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(project));
		when(studentRepository.findById(1L)).thenReturn(Optional.of(owner));
		when(studentRepository.findById(2L)).thenReturn(Optional.of(recipient));
		when(matchingService.recommendForProject(10L)).thenReturn(List.of(recommendation(2L)));
		when(teamRepository.findByProject_IdForUpdate(10L)).thenReturn(Optional.empty());
		when(teamRepository.save(any(Team.class))).thenReturn(team);
		when(teamMembershipRepository.save(any(TeamMembership.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(teamMembershipRepository.existsByTeam_IdAndStudent_Id(20L, 2L)).thenReturn(false);
		when(teamMembershipRepository.findByTeam_IdAndStudent_Id(20L, 1L))
				.thenReturn(Optional.of(new TeamMembership(team, owner, TeamMemberRole.LEADER)));
		when(teamMembershipRepository.countByTeam_IdAndStatus(20L, MembershipStatus.ACTIVE)).thenReturn(1L);
		when(teamRequestRepository.existsByRequestedTeam_IdAndRecipientStudent_IdAndStatusIn(
				20L, 2L, List.of(TeamRequestStatus.PENDING, TeamRequestStatus.ACCEPTED))).thenReturn(false);
		when(teamRequestRepository.countByRequestedTeam_IdAndStatus(20L, TeamRequestStatus.PENDING)).thenReturn(0L);
		when(teamRequestRepository.save(any(TeamRequest.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		TeamRequestResponse response = teamRequestService.create(1L, 10L, new TeamRequestCreateRequest(2L));

		assertEquals("Project", response.projectTitle());
		assertEquals("Project Team", response.teamName());
		assertEquals("INVITATION", response.requestType());
		assertEquals(TeamRequestStatus.PENDING, response.status());
		ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);
		verify(teamRepository).save(teamCaptor.capture());
		assertEquals("Project Team", teamCaptor.getValue().getTeamName());
		ArgumentCaptor<TeamMembership> membershipCaptor = ArgumentCaptor.forClass(TeamMembership.class);
		verify(teamMembershipRepository, times(1)).save(membershipCaptor.capture());
		assertEquals(owner, membershipCaptor.getValue().getStudent());
		assertEquals(TeamMemberRole.LEADER, membershipCaptor.getValue().getRole());
	}

	@Test
	void eligibleStudentCanSendJoinRequestToProjectOwner() {
		Student owner = student(1L, "Owner");
		Student requester = student(2L, "Requester");
		Project project = project(owner, 4);
		Team team = mock(Team.class);
		when(team.getId()).thenReturn(20L);
		when(team.getProject()).thenReturn(project);
		when(team.getTeamName()).thenReturn("Project Team");
		when(projectRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(project));
		when(studentRepository.findById(2L)).thenReturn(Optional.of(requester));
		when(matchingService.recommendForProject(10L)).thenReturn(List.of(recommendation(2L)));
		when(teamRepository.findByProject_IdForUpdate(10L)).thenReturn(Optional.of(team));
		when(teamMembershipRepository.findByTeam_IdAndStudent_Id(20L, 1L))
				.thenReturn(Optional.of(new TeamMembership(team, owner, TeamMemberRole.LEADER)));
		when(teamMembershipRepository.existsByTeam_IdAndStudent_Id(20L, 2L)).thenReturn(false);
		when(teamMembershipRepository.countByTeam_IdAndStatus(20L, MembershipStatus.ACTIVE)).thenReturn(1L);
		when(teamRequestRepository.existsByRequestedTeam_IdAndRecipientStudent_IdAndStatusIn(
				20L, 2L, List.of(TeamRequestStatus.PENDING, TeamRequestStatus.ACCEPTED))).thenReturn(false);
		when(teamRequestRepository.countByRequestedTeam_IdAndStatus(20L, TeamRequestStatus.PENDING)).thenReturn(0L);
		when(teamRequestRepository.save(any(TeamRequest.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		TeamRequestResponse response = teamRequestService.create(2L, 10L, new TeamRequestCreateRequest(1L));

		assertEquals("JOIN_REQUEST", response.requestType());
		assertEquals(2L, response.requestingStudentId());
		assertEquals(1L, response.recipientStudentId());
	}

	@Test
	void createRejectsRequestFromNonOwner() {
		Student owner = student(1L, "Owner");
		Student other = student(3L, "Other");
		Project project = project(owner, 4);
		when(projectRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(project));
		when(studentRepository.findById(3L)).thenReturn(Optional.of(other));

		assertThrows(ForbiddenOperationException.class,
				() -> teamRequestService.create(3L, 10L, new TeamRequestCreateRequest(2L)));
	}

	@Test
	void cancelledProjectRejectsNewJoinRequest() {
		Student owner = student(1L, "Owner");
		Student requester = student(2L, "Requester");
		Project project = project(owner, 4);
		when(project.getStatus()).thenReturn(ProjectStatus.CANCELLED);
		when(projectRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(project));

		assertThrows(DuplicateResourceException.class,
				() -> teamRequestService.create(2L, 10L, new TeamRequestCreateRequest(1L)));
		verifyNoInteractions(teamRepository, teamMembershipRepository, teamRequestRepository, matchingService);
	}

	@Test
	void cancelledProjectRejectsNewInvitation() {
		Student owner = student(1L, "Owner");
		Project project = project(owner, 4);
		when(project.getStatus()).thenReturn(ProjectStatus.CANCELLED);
		when(projectRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(project));

		assertThrows(DuplicateResourceException.class,
				() -> teamRequestService.create(1L, 10L, new TeamRequestCreateRequest(2L)));
		verifyNoInteractions(teamRepository, teamMembershipRepository, teamRequestRepository, matchingService);
	}

	@Test
	void pendingRequestForCancelledProjectCannotCreateMembership() {
		Student owner = student(1L, "Owner");
		Student recipient = student(2L, "Recipient");
		Project project = project(owner, 4);
		when(project.getStatus()).thenReturn(ProjectStatus.CANCELLED);
		Team team = team(project);
		TeamRequest request = new TeamRequest(recipient, owner, team);
		when(teamRequestRepository.findById(30L)).thenReturn(Optional.of(request));
		when(projectRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(project));
		when(teamRequestRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(request));
		when(studentRepository.existsById(1L)).thenReturn(true);

		assertThrows(DuplicateResourceException.class, () -> teamRequestService.respond(1L, 1L, 30L,
				new TeamRequestDecisionRequest(TeamRequestStatus.ACCEPTED)));
		verifyNoInteractions(teamMembershipRepository);
	}

	@Test
	void createRejectsDuplicatePendingInvitation() {
		Student owner = student(1L, "Owner");
		Student recipient = student(2L, "Recipient");
		Project project = project(owner, 4);
		Team team = mock(Team.class);
		when(team.getId()).thenReturn(20L);
		when(teamRepository.findByProject_IdForUpdate(10L)).thenReturn(Optional.of(team));
		when(projectRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(project));
		when(studentRepository.findById(1L)).thenReturn(Optional.of(owner));
		when(studentRepository.findById(2L)).thenReturn(Optional.of(recipient));
		when(matchingService.recommendForProject(10L)).thenReturn(List.of(recommendation(2L)));
		when(teamMembershipRepository.findByTeam_IdAndStudent_Id(20L, 1L))
				.thenReturn(Optional.of(new TeamMembership(team, owner, TeamMemberRole.LEADER)));
		when(teamMembershipRepository.existsByTeam_IdAndStudent_Id(20L, 2L)).thenReturn(false);
		when(teamRequestRepository.existsByRequestedTeam_IdAndRecipientStudent_IdAndStatusIn(
				20L, 2L, List.of(TeamRequestStatus.PENDING, TeamRequestStatus.ACCEPTED))).thenReturn(true);

		assertThrows(DuplicateResourceException.class,
				() -> teamRequestService.create(1L, 10L, new TeamRequestCreateRequest(2L)));
	}

	@Test
	void recipientAcceptingRequestActivatesMembershipAndFullTeam() {
		Student owner = student(1L, "Owner");
		Student recipient = student(2L, "Recipient");
		Project project = project(owner, 2);
		Team team = team(project);
		TeamRequest request = new TeamRequest(owner, recipient, team);
		when(teamRequestRepository.findById(30L)).thenReturn(Optional.of(request));
		when(projectRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(project));
		when(teamRequestRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(request));
		when(studentRepository.existsById(2L)).thenReturn(true);
		when(teamMembershipRepository.findByTeam_IdAndStudent_Id(20L, 1L))
				.thenReturn(Optional.of(new TeamMembership(team, owner, TeamMemberRole.LEADER)));
		when(teamMembershipRepository.countByTeam_IdAndStatus(20L, MembershipStatus.ACTIVE)).thenReturn(1L);
		when(teamMembershipRepository.findByTeam_IdAndStudent_Id(20L, 2L)).thenReturn(Optional.empty());
		when(teamMembershipRepository.save(any(TeamMembership.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(teamRequestRepository.save(request)).thenReturn(request);

		TeamRequestResponse response = teamRequestService.respond(2L, 2L, 30L,
				new TeamRequestDecisionRequest(TeamRequestStatus.ACCEPTED));

		assertEquals(TeamRequestStatus.ACCEPTED, response.status());
		verify(teamMembershipRepository).save(any(TeamMembership.class));
		verify(team).setStatus(TeamStatus.ACTIVE);
	}

	@Test
	void acceptanceReactivatesExistingMembershipInsteadOfDuplicatingIt() {
		Student owner = student(1L, "Owner");
		Student recipient = student(2L, "Recipient");
		Project project = project(owner, 4);
		Team team = team(project);
		TeamRequest request = new TeamRequest(owner, recipient, team);
		TeamMembership membership = new TeamMembership(team, recipient, TeamMemberRole.MEMBER);
		membership.setStatus(MembershipStatus.LEFT);
		when(teamRequestRepository.findById(30L)).thenReturn(Optional.of(request));
		when(projectRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(project));
		when(teamRequestRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(request));
		when(studentRepository.existsById(2L)).thenReturn(true);
		when(teamMembershipRepository.findByTeam_IdAndStudent_Id(20L, 1L))
				.thenReturn(Optional.of(new TeamMembership(team, owner, TeamMemberRole.LEADER)));
		when(teamMembershipRepository.countByTeam_IdAndStatus(20L, MembershipStatus.ACTIVE)).thenReturn(1L);
		when(teamMembershipRepository.findByTeam_IdAndStudent_Id(20L, 2L))
				.thenReturn(Optional.of(membership));
		when(teamMembershipRepository.save(membership)).thenReturn(membership);
		when(teamRequestRepository.save(request)).thenReturn(request);

		teamRequestService.respond(2L, 2L, 30L, new TeamRequestDecisionRequest(TeamRequestStatus.ACCEPTED));

		assertEquals(MembershipStatus.ACTIVE, membership.getStatus());
		verify(teamMembershipRepository).save(membership);
	}

	@Test
	void anotherStudentCannotRespondToRequest() {
		Student owner = student(1L, "Owner");
		Student recipient = student(2L, "Recipient");
		Project project = project(owner, 4);
		TeamRequest request = new TeamRequest(owner, recipient, team(project));
		when(teamRequestRepository.findById(30L)).thenReturn(Optional.of(request));
		when(projectRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(project));
		when(teamRequestRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(request));
		when(studentRepository.existsById(3L)).thenReturn(true);

		assertThrows(ForbiddenOperationException.class, () -> teamRequestService.respond(3L, 3L, 30L,
				new TeamRequestDecisionRequest(TeamRequestStatus.ACCEPTED)));
	}

	@Test
	void authenticatedStudentCannotReadAnotherStudentsRequests() {
		assertThrows(ForbiddenOperationException.class,
				() -> teamRequestService.findIncoming(2L, 3L));
		verifyNoInteractions(teamRequestRepository);
	}

	@Test
	void incomingRequestsAreFilteredToPendingRequestsForAuthenticatedRecipient() {
		Student owner = student(1L, "Owner");
		Student recipient = student(2L, "Recipient");
		TeamRequest pending = new TeamRequest(owner, recipient, team(project(owner, 4)));
		pending.setStatus(TeamRequestStatus.PENDING);
		when(studentRepository.existsById(2L)).thenReturn(true);
		when(teamRequestRepository.findAllByRecipientStudent_IdAndStatusOrderByCreatedAtDesc(
				2L, TeamRequestStatus.PENDING)).thenReturn(List.of(pending));

		List<TeamRequestResponse> received = teamRequestService.findIncoming(2L, 2L);

		assertEquals(1, received.size());
		assertEquals(TeamRequestStatus.PENDING, received.get(0).status());
		assertEquals(2L, received.get(0).recipientStudentId());
		verify(teamRequestRepository).findAllByRecipientStudent_IdAndStatusOrderByCreatedAtDesc(
				2L, TeamRequestStatus.PENDING);
	}

	@Test
	void incomingHistoryContainsOnlyAnsweredStatusesForAuthenticatedRecipient() {
		Student owner = student(1L, "Owner");
		Student recipient = student(2L, "Recipient");
		TeamRequest accepted = new TeamRequest(owner, recipient, team(project(owner, 4)));
		accepted.setStatus(TeamRequestStatus.ACCEPTED);
		TeamRequest rejected = new TeamRequest(owner, recipient, team(project(owner, 4)));
		rejected.setStatus(TeamRequestStatus.REJECTED);
		TeamRequest withdrawn = new TeamRequest(owner, recipient, team(project(owner, 4)));
		withdrawn.setStatus(TeamRequestStatus.WITHDRAWN);
		List<TeamRequestStatus> answeredStatuses = List.of(
				TeamRequestStatus.ACCEPTED, TeamRequestStatus.REJECTED, TeamRequestStatus.WITHDRAWN);
		when(studentRepository.existsById(2L)).thenReturn(true);
		when(teamRequestRepository.findAllByRecipientStudent_IdAndStatusInOrderByCreatedAtDesc(
				2L, answeredStatuses)).thenReturn(List.of(accepted, rejected, withdrawn));

		List<TeamRequestResponse> history = teamRequestService.findIncomingHistory(2L, 2L);

		assertEquals(List.of(TeamRequestStatus.ACCEPTED, TeamRequestStatus.REJECTED, TeamRequestStatus.WITHDRAWN),
				history.stream().map(TeamRequestResponse::status).toList());
		verify(teamRequestRepository).findAllByRecipientStudent_IdAndStatusInOrderByCreatedAtDesc(
				2L, answeredStatuses);
	}

	@Test
	void anotherStudentCannotReadIncomingRequestHistory() {
		assertThrows(ForbiddenOperationException.class,
				() -> teamRequestService.findIncomingHistory(2L, 3L));
		verifyNoInteractions(teamRequestRepository);
	}

	@Test
	void requestCannotBeAnsweredMoreThanOnce() {
		Student owner = student(1L, "Owner");
		Student recipient = student(2L, "Recipient");
		Project project = project(owner, 4);
		TeamRequest request = new TeamRequest(owner, recipient, team(project));
		request.setStatus(TeamRequestStatus.ACCEPTED);
		when(teamRequestRepository.findById(30L)).thenReturn(Optional.of(request));
		when(projectRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(project));
		when(teamRequestRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(request));
		when(studentRepository.existsById(2L)).thenReturn(true);

		assertThrows(DuplicateResourceException.class, () -> teamRequestService.respond(2L, 2L, 30L,
				new TeamRequestDecisionRequest(TeamRequestStatus.REJECTED)));
	}

	@Test
	void acceptanceRejectsFullTeam() {
		Student owner = student(1L, "Owner");
		Student recipient = student(2L, "Recipient");
		Project project = project(owner, 2);
		Team team = team(project);
		TeamRequest request = new TeamRequest(owner, recipient, team);
		when(teamRequestRepository.findById(30L)).thenReturn(Optional.of(request));
		when(projectRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(project));
		when(teamRequestRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(request));
		when(studentRepository.existsById(2L)).thenReturn(true);
		when(teamMembershipRepository.findByTeam_IdAndStudent_Id(20L, 1L))
				.thenReturn(Optional.of(new TeamMembership(team, owner, TeamMemberRole.LEADER)));
		when(teamMembershipRepository.countByTeam_IdAndStatus(20L, MembershipStatus.ACTIVE)).thenReturn(2L);

		assertThrows(DuplicateResourceException.class, () -> teamRequestService.respond(2L, 2L, 30L,
				new TeamRequestDecisionRequest(TeamRequestStatus.ACCEPTED)));
	}

	@Test
	void projectOwnerCanRejectJoinRequestWithoutCreatingMembership() {
		Student owner = student(1L, "Owner");
		Student applicant = student(2L, "Applicant");
		Project project = project(owner, 4);
		Team team = team(project);
		TeamRequest request = new TeamRequest(applicant, owner, team);
		when(teamRequestRepository.findById(30L)).thenReturn(Optional.of(request));
		when(projectRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(project));
		when(teamRequestRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(request));
		when(studentRepository.existsById(1L)).thenReturn(true);
		when(teamRequestRepository.save(request)).thenReturn(request);

		TeamRequestResponse response = teamRequestService.respond(1L, 1L, 30L,
				new TeamRequestDecisionRequest(TeamRequestStatus.REJECTED));

		assertEquals(TeamRequestStatus.REJECTED, response.status());
		org.mockito.Mockito.verifyNoInteractions(teamMembershipRepository);
	}

	@Test
	void studentCanRequestToJoinAndOwnerAcceptanceAddsApplicantAsMember() {
		Student owner = student(1L, "Owner");
		Student applicant = student(2L, "Applicant");
		Project project = project(owner, 4);
		Team team = mock(Team.class);
		when(team.getId()).thenReturn(20L);
		when(team.getProject()).thenReturn(project);
		when(projectRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(project));
		when(studentRepository.findById(2L)).thenReturn(Optional.of(applicant));
		when(matchingService.recommendForProject(10L)).thenReturn(List.of(recommendation(2L)));
		when(teamRepository.findByProject_IdForUpdate(10L)).thenReturn(Optional.empty());
		when(teamRepository.save(any(Team.class))).thenReturn(team);
		when(teamMembershipRepository.save(any(TeamMembership.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(teamMembershipRepository.findByTeam_IdAndStudent_Id(20L, 1L))
				.thenReturn(Optional.of(new TeamMembership(team, owner, TeamMemberRole.LEADER)));
		when(teamMembershipRepository.existsByTeam_IdAndStudent_Id(20L, 2L)).thenReturn(false);
		when(teamMembershipRepository.countByTeam_IdAndStatus(20L, MembershipStatus.ACTIVE)).thenReturn(1L);
		when(teamRequestRepository.existsByRequestedTeam_IdAndRecipientStudent_IdAndStatusIn(
				20L, 2L, List.of(TeamRequestStatus.PENDING, TeamRequestStatus.ACCEPTED))).thenReturn(false);
		when(teamRequestRepository.existsByRequestedTeam_IdAndRequestingStudent_IdAndRecipientStudent_IdAndStatusIn(
				20L, 2L, 1L, List.of(TeamRequestStatus.PENDING, TeamRequestStatus.ACCEPTED))).thenReturn(false);
		when(teamRequestRepository.countByRequestedTeam_IdAndStatus(20L, TeamRequestStatus.PENDING)).thenReturn(0L);
		when(teamRequestRepository.save(any(TeamRequest.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		TeamRequestResponse created = teamRequestService.create(2L, 10L, new TeamRequestCreateRequest(1L));
		TeamRequest joinRequest = new TeamRequest(applicant, owner, team);
		when(teamRequestRepository.findById(30L)).thenReturn(Optional.of(joinRequest));
		when(teamRequestRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(joinRequest));
		when(studentRepository.existsById(1L)).thenReturn(true);
		when(teamMembershipRepository.findByTeam_IdAndStudent_Id(20L, 2L)).thenReturn(Optional.empty());
		when(teamMembershipRepository.countByTeam_IdAndStatus(20L, MembershipStatus.ACTIVE)).thenReturn(1L);
		when(teamMembershipRepository.save(any(TeamMembership.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(teamRequestRepository.save(joinRequest)).thenReturn(joinRequest);

		assertEquals(2L, created.requestingStudentId());
		assertEquals(1L, created.recipientStudentId());
		TeamRequestResponse accepted = teamRequestService.respond(1L, 1L, 30L,
				new TeamRequestDecisionRequest(TeamRequestStatus.ACCEPTED));

		assertEquals(TeamRequestStatus.ACCEPTED, accepted.status());
		ArgumentCaptor<TeamMembership> membershipCaptor = ArgumentCaptor.forClass(TeamMembership.class);
		verify(teamMembershipRepository, times(2)).save(membershipCaptor.capture());
		assertEquals(applicant, membershipCaptor.getAllValues().get(1).getStudent());
		assertEquals(TeamMemberRole.MEMBER, membershipCaptor.getAllValues().get(1).getRole());
	}

	@Test
	void duplicateJoinRequestsAreRejected() {
		Student owner = student(1L, "Owner");
		Student applicant = student(2L, "Applicant");
		Project project = project(owner, 4);
		Team team = team(project);
		when(projectRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(project));
		when(studentRepository.findById(2L)).thenReturn(Optional.of(applicant));
		when(matchingService.recommendForProject(10L)).thenReturn(List.of(recommendation(2L)));
		when(teamRepository.findByProject_IdForUpdate(10L)).thenReturn(Optional.of(team));
		when(teamMembershipRepository.findByTeam_IdAndStudent_Id(20L, 1L))
				.thenReturn(Optional.of(new TeamMembership(team, owner, TeamMemberRole.LEADER)));
		when(teamMembershipRepository.existsByTeam_IdAndStudent_Id(20L, 2L)).thenReturn(false);
		when(teamRequestRepository.existsByRequestedTeam_IdAndRecipientStudent_IdAndStatusIn(
				20L, 2L, List.of(TeamRequestStatus.PENDING, TeamRequestStatus.ACCEPTED))).thenReturn(false);
		when(teamRequestRepository.existsByRequestedTeam_IdAndRequestingStudent_IdAndRecipientStudent_IdAndStatusIn(
				20L, 2L, 1L, List.of(TeamRequestStatus.PENDING, TeamRequestStatus.ACCEPTED))).thenReturn(true);

		assertThrows(DuplicateResourceException.class,
				() -> teamRequestService.create(2L, 10L, new TeamRequestCreateRequest(1L)));
	}

	@Test
	void invitationIsRejectedWhenTheStudentAlreadyRequestedToJoin() {
		Student owner = student(1L, "Owner");
		Student applicant = student(2L, "Applicant");
		Project project = project(owner, 4);
		Team team = team(project);
		when(projectRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(project));
		when(studentRepository.findById(1L)).thenReturn(Optional.of(owner));
		when(studentRepository.findById(2L)).thenReturn(Optional.of(applicant));
		when(matchingService.recommendForProject(10L)).thenReturn(List.of(recommendation(2L)));
		when(teamRepository.findByProject_IdForUpdate(10L)).thenReturn(Optional.of(team));
		when(teamMembershipRepository.findByTeam_IdAndStudent_Id(20L, 1L))
				.thenReturn(Optional.of(new TeamMembership(team, owner, TeamMemberRole.LEADER)));
		when(teamMembershipRepository.existsByTeam_IdAndStudent_Id(20L, 2L)).thenReturn(false);
		when(teamRequestRepository.existsByRequestedTeam_IdAndRecipientStudent_IdAndStatusIn(
				20L, 2L, List.of(TeamRequestStatus.PENDING, TeamRequestStatus.ACCEPTED))).thenReturn(false);
		when(teamRequestRepository.existsByRequestedTeam_IdAndRequestingStudent_IdAndRecipientStudent_IdAndStatusIn(
				20L, 1L, 2L, List.of(TeamRequestStatus.PENDING, TeamRequestStatus.ACCEPTED))).thenReturn(false);
		when(teamRequestRepository.existsByRequestedTeam_IdAndRequestingStudent_IdAndRecipientStudent_IdAndStatusIn(
				20L, 2L, 1L, List.of(TeamRequestStatus.PENDING, TeamRequestStatus.ACCEPTED))).thenReturn(true);

		assertThrows(DuplicateResourceException.class,
				() -> teamRequestService.create(1L, 10L, new TeamRequestCreateRequest(2L)));
	}

	private Project project(Student owner, int teamSize) {
		Project project = mock(Project.class);
		lenient().when(project.getId()).thenReturn(10L);
		lenient().when(project.getTitle()).thenReturn("Project");
		lenient().when(project.getOwner()).thenReturn(owner);
		lenient().when(project.getRequiredTeamSize()).thenReturn(teamSize);
		lenient().when(project.getStatus()).thenReturn(ProjectStatus.OPEN);
		return project;
	}

	private Team team(Project project) {
		Team team = mock(Team.class);
		lenient().when(team.getId()).thenReturn(20L);
		lenient().when(team.getProject()).thenReturn(project);
		lenient().when(team.getStatus()).thenReturn(TeamStatus.FORMING);
		return team;
	}

	private Student student(Long id, String name) {
		Student student = mock(Student.class);
		lenient().when(student.getId()).thenReturn(id);
		lenient().when(student.getName()).thenReturn(name);
		lenient().when(student.isAvailable()).thenReturn(true);
		return student;
	}

	private StudentRecommendationResponse recommendation(Long studentId) {
		return new StudentRecommendationResponse(studentId, "Recipient", "recipient@example.com", "CSE",
				2, 100.0, List.of("Java"), List.of());
	}
}

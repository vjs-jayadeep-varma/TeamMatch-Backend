package com.teammatch.backend.service;

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
import com.teammatch.backend.exception.ResourceNotFoundException;
import com.teammatch.backend.repository.ProjectRepository;
import com.teammatch.backend.repository.StudentRepository;
import com.teammatch.backend.repository.TeamMembershipRepository;
import com.teammatch.backend.repository.TeamRepository;
import com.teammatch.backend.repository.TeamRequestRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TeamRequestService {
	private static final List<TeamRequestStatus> ACTIVE_REQUEST_STATUSES = List.of(
			TeamRequestStatus.PENDING, TeamRequestStatus.ACCEPTED);
	private static final List<TeamRequestStatus> ANSWERED_REQUEST_STATUSES = List.of(
			TeamRequestStatus.ACCEPTED, TeamRequestStatus.REJECTED, TeamRequestStatus.WITHDRAWN);

	private final ProjectRepository projectRepository;
	private final StudentRepository studentRepository;
	private final TeamRepository teamRepository;
	private final TeamMembershipRepository teamMembershipRepository;
	private final TeamRequestRepository teamRequestRepository;
	private final MatchingService matchingService;

	public TeamRequestService(ProjectRepository projectRepository, StudentRepository studentRepository,
			TeamRepository teamRepository, TeamMembershipRepository teamMembershipRepository,
			TeamRequestRepository teamRequestRepository, MatchingService matchingService) {
		this.projectRepository = projectRepository;
		this.studentRepository = studentRepository;
		this.teamRepository = teamRepository;
		this.teamMembershipRepository = teamMembershipRepository;
		this.teamRequestRepository = teamRequestRepository;
		this.matchingService = matchingService;
	}

	public TeamRequestResponse create(Long requestingStudentId, Long projectId, TeamRequestCreateRequest request) {
		Project project = projectRepository.findByIdForUpdate(projectId)
				.orElseThrow(() -> new ResourceNotFoundException("Project " + projectId + " was not found."));
		if (project.getStatus() != ProjectStatus.OPEN && project.getStatus() != ProjectStatus.IN_PROGRESS) {
			throw new DuplicateResourceException("Team requests are not available for this project.");
		}

		Student requester = studentRepository.findById(requestingStudentId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Student " + requestingStudentId + " was not found."));
		boolean ownerInvitation = requester.getId().equals(project.getOwner().getId());
		Student recipient;
		Student prospectiveMember;
		if (ownerInvitation) {
			recipient = studentRepository.findById(request.recipientStudentId())
					.orElseThrow(() -> new ResourceNotFoundException(
							"Student " + request.recipientStudentId() + " was not found."));
			prospectiveMember = recipient;
			if (requester.getId().equals(recipient.getId())) {
				throw new DuplicateResourceException("A student cannot invite themselves to their own project.");
			}
		} else {
			if (!project.getOwner().getId().equals(request.recipientStudentId())) {
				throw new ForbiddenOperationException(
						"A student requesting to join must address the request to the project owner.");
			}
			recipient = project.getOwner();
			prospectiveMember = requester;
		}
		if (!prospectiveMember.isAvailable() || matchingService.recommendForProject(projectId).stream()
				.map(StudentRecommendationResponse::studentId)
				.noneMatch(prospectiveMember.getId()::equals)) {
			throw new DuplicateResourceException("The selected student is not currently eligible for this project.");
		}

		Team team = teamRepository.findByProject_IdForUpdate(projectId).orElseGet(() -> {
			String teamName = project.getTitle() + " Team";
			if (teamName.length() > 100) {
				teamName = teamName.substring(0, 100);
			}
			Team newTeam = teamRepository.save(new Team(project, teamName));
			teamMembershipRepository.save(new TeamMembership(newTeam, project.getOwner(), TeamMemberRole.LEADER));
			return newTeam;
		});

		if (team.getStatus() == TeamStatus.DISBANDED) {
			throw new DuplicateResourceException("This team is no longer accepting members.");
		}
		var leaderMembership = teamMembershipRepository.findByTeam_IdAndStudent_Id(
				team.getId(), project.getOwner().getId());
		if (leaderMembership.isEmpty() || leaderMembership.get().getStatus() != MembershipStatus.ACTIVE
				|| leaderMembership.get().getRole() != TeamMemberRole.LEADER) {
			throw new DuplicateResourceException("The project team does not have an active owner leader.");
		}
		if (teamMembershipRepository.existsByTeam_IdAndStudent_Id(team.getId(), prospectiveMember.getId())) {
			throw new DuplicateResourceException("The selected student is already a member of this team.");
		}
		if (teamRequestRepository.existsByRequestedTeam_IdAndRecipientStudent_IdAndStatusIn(
				team.getId(), prospectiveMember.getId(), ACTIVE_REQUEST_STATUSES)
				|| teamRequestRepository
						.existsByRequestedTeam_IdAndRequestingStudent_IdAndRecipientStudent_IdAndStatusIn(
								team.getId(), requester.getId(), recipient.getId(), ACTIVE_REQUEST_STATUSES)
				|| teamRequestRepository
						.existsByRequestedTeam_IdAndRequestingStudent_IdAndRecipientStudent_IdAndStatusIn(
								team.getId(), recipient.getId(), requester.getId(), ACTIVE_REQUEST_STATUSES)) {
			throw new DuplicateResourceException("A pending or accepted request already exists for this student.");
		}

		long activeMembers = teamMembershipRepository.countByTeam_IdAndStatus(team.getId(), MembershipStatus.ACTIVE);
		long pendingRequests = teamRequestRepository.countByRequestedTeam_IdAndStatus(
				team.getId(), TeamRequestStatus.PENDING);
		if (activeMembers + pendingRequests >= project.getRequiredTeamSize()) {
			throw new DuplicateResourceException("The project has no remaining team spaces.");
		}

		return toResponse(teamRequestRepository.save(new TeamRequest(requester, recipient, team)));
	}

	@Transactional(readOnly = true)
	public List<TeamRequestResponse> findIncoming(Long authenticatedStudentId, Long requestedStudentId) {
		requireSubject(authenticatedStudentId, requestedStudentId);
		return teamRequestRepository.findAllByRecipientStudent_IdAndStatusOrderByCreatedAtDesc(
						authenticatedStudentId, TeamRequestStatus.PENDING).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<TeamRequestResponse> findIncomingHistory(Long authenticatedStudentId, Long requestedStudentId) {
		requireSubject(authenticatedStudentId, requestedStudentId);
		return teamRequestRepository.findAllByRecipientStudent_IdAndStatusInOrderByCreatedAtDesc(
						authenticatedStudentId, ANSWERED_REQUEST_STATUSES).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<TeamRequestResponse> findOutgoing(Long authenticatedStudentId, Long requestedStudentId) {
		requireSubject(authenticatedStudentId, requestedStudentId);
		return teamRequestRepository.findAllByRequestingStudent_IdAndStatusOrderByCreatedAtDesc(
						authenticatedStudentId, TeamRequestStatus.PENDING).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<TeamRequestResponse> findOutgoingHistory(Long authenticatedStudentId, Long requestedStudentId) {
		requireSubject(authenticatedStudentId, requestedStudentId);
		return teamRequestRepository.findAllByRequestingStudent_IdAndStatusInOrderByCreatedAtDesc(
						authenticatedStudentId, ANSWERED_REQUEST_STATUSES).stream()
				.map(this::toResponse)
				.toList();
	}

	public TeamRequestResponse respond(Long authenticatedStudentId, Long requestedStudentId, Long requestId,
			TeamRequestDecisionRequest decision) {
		requireSubject(authenticatedStudentId, requestedStudentId);
		TeamRequest existing = teamRequestRepository.findById(requestId)
				.orElseThrow(() -> new ResourceNotFoundException("Team request " + requestId + " was not found."));
		Long projectId = existing.getRequestedTeam().getProject().getId();
		Project project = projectRepository.findByIdForUpdate(projectId)
				.orElseThrow(() -> new ResourceNotFoundException("Project " + projectId + " was not found."));
		TeamRequest request = teamRequestRepository.findByIdForUpdate(requestId)
				.orElseThrow(() -> new ResourceNotFoundException("Team request " + requestId + " was not found."));

		if (!request.getRecipientStudent().getId().equals(authenticatedStudentId)) {
			throw new ForbiddenOperationException("Only the invited student can respond to this request.");
		}
		if (request.getStatus() != TeamRequestStatus.PENDING) {
			throw new DuplicateResourceException("This team request has already been answered.");
		}
		if (decision.status() != TeamRequestStatus.ACCEPTED && decision.status() != TeamRequestStatus.REJECTED) {
			throw new DuplicateResourceException("A team request can only be accepted or rejected.");
		}

		if (decision.status() == TeamRequestStatus.ACCEPTED) {
			Team team = request.getRequestedTeam();
			if (project.getStatus() != ProjectStatus.OPEN && project.getStatus() != ProjectStatus.IN_PROGRESS) {
				throw new DuplicateResourceException("This project is no longer accepting team members.");
			}
			if (team.getStatus() == TeamStatus.DISBANDED) {
				throw new DuplicateResourceException("This team is no longer accepting members.");
			}
			var leader = teamMembershipRepository.findByTeam_IdAndStudent_Id(team.getId(), project.getOwner().getId());
			if (leader.isEmpty() || leader.get().getStatus() != MembershipStatus.ACTIVE
					|| leader.get().getRole() != TeamMemberRole.LEADER) {
				throw new DuplicateResourceException("The project team does not have an active owner leader.");
			}
			long activeMembers = teamMembershipRepository.countByTeam_IdAndStatus(team.getId(), MembershipStatus.ACTIVE);
			if (activeMembers >= project.getRequiredTeamSize()) {
				throw new DuplicateResourceException("The project team is already full.");
			}

			Student joiningStudent = request.getRequestingStudent().getId().equals(project.getOwner().getId())
					? request.getRecipientStudent()
					: request.getRequestingStudent();
			TeamMembership membership = teamMembershipRepository
					.findByTeam_IdAndStudent_Id(team.getId(), joiningStudent.getId())
					.orElseGet(() -> new TeamMembership(team, joiningStudent, TeamMemberRole.MEMBER));
			if (membership.getRole() != TeamMemberRole.MEMBER) {
				throw new DuplicateResourceException("The requested student has a conflicting team membership role.");
			}
			if (membership.getStatus() != MembershipStatus.ACTIVE) {
				membership.setStatus(MembershipStatus.ACTIVE);
			}
			teamMembershipRepository.save(membership);
			if (activeMembers + 1 >= project.getRequiredTeamSize()) {
				team.setStatus(TeamStatus.ACTIVE);
			}
		}

		request.setStatus(decision.status());
		return toResponse(teamRequestRepository.save(request));
	}

	private void requireSubject(Long authenticatedStudentId, Long requestedStudentId) {
		if (authenticatedStudentId == null) {
			throw new ForbiddenOperationException("A student account is required for team request actions.");
		}
		if (!authenticatedStudentId.equals(requestedStudentId)) {
			throw new ForbiddenOperationException("You can only access your own team requests.");
		}
		if (!studentRepository.existsById(authenticatedStudentId)) {
			throw new ResourceNotFoundException("Student " + authenticatedStudentId + " was not found.");
		}
	}

	private TeamRequestResponse toResponse(TeamRequest request) {
		Team team = request.getRequestedTeam();
		Project project = team.getProject();
		Student requester = request.getRequestingStudent();
		Student recipient = request.getRecipientStudent();
		return new TeamRequestResponse(request.getId(), project.getId(), project.getTitle(), team.getTeamName(),
				requester.getId().equals(project.getOwner().getId()) ? "INVITATION" : "JOIN_REQUEST",
				requester.getId(), requester.getName(), recipient.getId(), recipient.getName(), request.getStatus(),
				request.getCreatedAt(), request.getUpdatedAt());
	}
}

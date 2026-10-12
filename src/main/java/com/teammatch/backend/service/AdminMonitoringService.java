package com.teammatch.backend.service;

import com.teammatch.backend.dto.AdminMonitoringResponse;
import com.teammatch.backend.dto.AdminOverviewResponse;
import com.teammatch.backend.dto.AdminPage;
import com.teammatch.backend.dto.AdminProjectDetail;
import com.teammatch.backend.dto.AdminProjectSummary;
import com.teammatch.backend.dto.AdminStudentDetail;
import com.teammatch.backend.dto.AdminStudentListItem;
import com.teammatch.backend.dto.AdminStudentMembership;
import com.teammatch.backend.dto.AdminTeamRequestDetail;
import com.teammatch.backend.dto.AdminTeamMemberRow;
import com.teammatch.backend.dto.AdminTeamMemberSummary;
import com.teammatch.backend.dto.AdminTeamSummary;
import com.teammatch.backend.entity.Project;
import com.teammatch.backend.entity.Student;
import com.teammatch.backend.entity.Team;
import com.teammatch.backend.entity.TeamMembership;
import com.teammatch.backend.entity.TeamRequest;
import com.teammatch.backend.entity.enums.TeamRequestStatus;
import com.teammatch.backend.entity.enums.TeamStatus;
import com.teammatch.backend.exception.ResourceNotFoundException;
import com.teammatch.backend.repository.ProjectRepository;
import com.teammatch.backend.repository.StudentRepository;
import com.teammatch.backend.repository.StudentSkillRepository;
import com.teammatch.backend.repository.TeamMembershipRepository;
import com.teammatch.backend.repository.TeamRepository;
import com.teammatch.backend.repository.TeamRequestRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminMonitoringService {
	private final StudentRepository studentRepository;
	private final ProjectRepository projectRepository;
	private final TeamRepository teamRepository;
	private final TeamRequestRepository teamRequestRepository;
	private final StudentSkillRepository studentSkillRepository;
	private final TeamMembershipRepository teamMembershipRepository;

	public AdminMonitoringService(
			StudentRepository studentRepository,
			ProjectRepository projectRepository,
			TeamRepository teamRepository,
			TeamRequestRepository teamRequestRepository,
			StudentSkillRepository studentSkillRepository,
			TeamMembershipRepository teamMembershipRepository) {
		this.studentRepository = studentRepository;
		this.projectRepository = projectRepository;
		this.teamRepository = teamRepository;
		this.teamRequestRepository = teamRequestRepository;
		this.studentSkillRepository = studentSkillRepository;
		this.teamMembershipRepository = teamMembershipRepository;
	}

	@Transactional(readOnly = true)
	public AdminOverviewResponse getOverview() {
		Map<String, Long> requestsByStatus = new LinkedHashMap<>();
		for (TeamRequestStatus status : TeamRequestStatus.values()) {
			requestsByStatus.put(status.name(), teamRequestRepository.countByStatus(status));
		}
		return new AdminOverviewResponse(studentRepository.count(), projectRepository.count(),
				teamRepository.count(), teamRequestRepository.count(), Map.copyOf(requestsByStatus));
	}

	@Transactional(readOnly = true)
	public AdminPage<AdminStudentListItem> findStudents(String query, int page, int size) {
		Page<Student> students = studentRepository.searchAdminStudents(query, pageRequest(page, size, "id"));
		return AdminPage.from(students.map(student -> new AdminStudentListItem(
				student.getId(), student.getName(), student.getRollNumber(), student.getEmail(),
				student.getDepartment(), student.getAcademicYear())));
	}

	@Transactional(readOnly = true)
	public AdminStudentDetail findStudent(Long studentId) {
		Student student = studentRepository.findById(studentId)
				.orElseThrow(() -> new ResourceNotFoundException("Student " + studentId + " was not found."));
		List<String> skills = studentSkillRepository.findAllByStudent_Id(studentId).stream()
				.map(studentSkill -> studentSkill.getSkill().getName())
				.sorted(String.CASE_INSENSITIVE_ORDER)
				.toList();
		List<AdminProjectSummary> projects = projectRepository.findAllByOwner_Id(studentId).stream()
				.map(this::projectSummary)
				.toList();
		List<AdminStudentMembership> memberships = teamMembershipRepository.findAllByStudent_Id(studentId).stream()
				.map(this::studentMembership)
				.toList();
		return new AdminStudentDetail(student.getId(), student.getName(), student.getRollNumber(), student.getEmail(),
				student.getDepartment(), student.getAcademicYear(), student.isAvailable(), skills,
				student.getInterests().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(),
				projects, memberships);
	}

	@Transactional(readOnly = true)
	public AdminPage<AdminProjectSummary> findProjects(String query, int page, int size) {
		Page<Project> projects = projectRepository.searchAdminProjects(query, pageRequest(page, size, "id"));
		return AdminPage.from(projects.map(this::projectSummary));
	}

	@Transactional(readOnly = true)
	public AdminProjectDetail findProject(Long projectId) {
		Project project = projectRepository.findById(projectId)
				.orElseThrow(() -> new ResourceNotFoundException("Project " + projectId + " was not found."));
		Team team = teamRepository.findByProject_Id(projectId).orElse(null);
		List<AdminTeamMemberSummary> members = team == null ? List.of()
				: teamMembershipRepository.findAllByTeam_Id(team.getId()).stream()
						.map(this::teamMember)
						.toList();
		return new AdminProjectDetail(project.getId(), project.getTitle(), project.getDescription(),
				project.getStatus(), project.getRequiredTeamSize(), project.getOwner().getId(),
				project.getOwner().getName(),
				project.getRequiredSkills().stream().map(skill -> skill.getName())
						.sorted(String.CASE_INSENSITIVE_ORDER).toList(),
				team == null ? null : team.getId(), team == null ? null : team.getTeamName(),
				team == null ? null : team.getStatus(), members);
	}

	@Transactional(readOnly = true)
	public AdminPage<AdminTeamSummary> findTeams(int page, int size) {
		Page<Team> teams = teamRepository.findAll(pageRequest(page, size, "id"));
		return AdminPage.from(teams.map(this::teamSummary));
	}

	@Transactional(readOnly = true)
	public AdminPage<AdminTeamRequestDetail> findRequests(
			TeamRequestStatus status, Long projectId, int page, int size) {
		Page<TeamRequest> requests = teamRequestRepository.findAdminRequests(
				status, projectId, pageRequest(page, size, "createdAt", Sort.Direction.DESC));
		return AdminPage.from(requests.map(this::requestDetail));
	}

	@Transactional(readOnly = true)
	public AdminMonitoringResponse getMonitoringData() {
		Map<Long, TeamAccumulator> teams = new LinkedHashMap<>();
		for (AdminTeamMemberRow row : teamRepository.findAllAdminSummaries()) {
			TeamAccumulator team = teams.computeIfAbsent(row.teamId(), ignored -> new TeamAccumulator(row));
			if (row.studentId() != null) {
				team.members().add(new AdminTeamMemberSummary(
						row.studentId(), row.studentName(), row.memberRole(), row.membershipStatus()));
			}
		}

		List<AdminTeamSummary> teamSummaries = teams.values().stream()
				.map(team -> new AdminTeamSummary(team.id(), team.name(), team.status(), team.projectId(),
						team.projectTitle(), List.copyOf(team.members())))
				.toList();

		return new AdminMonitoringResponse(
				studentRepository.findAllAdminSummaries(),
				projectRepository.findAllAdminSummaries(),
				teamSummaries,
				teamRequestRepository.findAllAdminSummaries());
	}

	private AdminProjectSummary projectSummary(Project project) {
		return new AdminProjectSummary(project.getId(), project.getTitle(), project.getStatus(),
				project.getRequiredTeamSize(), project.getOwner().getName());
	}

	private AdminStudentMembership studentMembership(TeamMembership membership) {
		Team team = membership.getTeam();
		Project project = team.getProject();
		return new AdminStudentMembership(team.getId(), team.getTeamName(), project.getId(), project.getTitle(),
				membership.getRole(), membership.getStatus());
	}

	private AdminTeamMemberSummary teamMember(TeamMembership membership) {
		return new AdminTeamMemberSummary(membership.getStudent().getId(), membership.getStudent().getName(),
				membership.getRole(), membership.getStatus());
	}

	private AdminTeamSummary teamSummary(Team team) {
		List<AdminTeamMemberSummary> members = teamMembershipRepository.findAllByTeam_Id(team.getId()).stream()
				.map(this::teamMember)
				.toList();
		return new AdminTeamSummary(team.getId(), team.getTeamName(), team.getStatus(),
				team.getProject().getId(), team.getProject().getTitle(), members);
	}

	private AdminTeamRequestDetail requestDetail(TeamRequest request) {
		Team team = request.getRequestedTeam();
		Project project = team.getProject();
		return new AdminTeamRequestDetail(request.getId(), project.getId(), project.getTitle(),
				team.getId(), team.getTeamName(),
				request.getRequestingStudent().getId(), request.getRequestingStudent().getName(),
				request.getRequestingStudent().getId().equals(project.getOwner().getId())
						? "INVITATION" : "JOIN_REQUEST",
				request.getRecipientStudent().getId(), request.getRecipientStudent().getName(),
				request.getStatus(), request.getCreatedAt(), request.getUpdatedAt());
	}

	private PageRequest pageRequest(int page, int size, String sortField) {
		return pageRequest(page, size, sortField, Sort.Direction.ASC);
	}

	private PageRequest pageRequest(int page, int size, String sortField, Sort.Direction direction) {
		return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)),
				Sort.by(direction, sortField));
	}

	private static final class TeamAccumulator {
		private final Long id;
		private final String name;
		private final TeamStatus status;
		private final Long projectId;
		private final String projectTitle;
		private final List<AdminTeamMemberSummary> members = new ArrayList<>();

		private TeamAccumulator(AdminTeamMemberRow row) {
			this.id = row.teamId();
			this.name = row.teamName();
			this.status = row.teamStatus();
			this.projectId = row.projectId();
			this.projectTitle = row.projectTitle();
		}

		private Long id() { return id; }
		private String name() { return name; }
		private TeamStatus status() { return status; }
		private Long projectId() { return projectId; }
		private String projectTitle() { return projectTitle; }
		private List<AdminTeamMemberSummary> members() { return members; }
	}
}

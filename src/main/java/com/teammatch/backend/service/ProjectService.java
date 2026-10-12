package com.teammatch.backend.service;

import com.teammatch.backend.dto.ProjectCreateRequest;
import com.teammatch.backend.dto.ProjectResponse;
import com.teammatch.backend.dto.ProjectTeamMemberResponse;
import com.teammatch.backend.dto.SkillResponse;
import com.teammatch.backend.entity.Project;
import com.teammatch.backend.entity.Skill;
import com.teammatch.backend.entity.Student;
import com.teammatch.backend.entity.enums.ProjectStatus;
import com.teammatch.backend.entity.enums.TeamMemberRole;
import com.teammatch.backend.entity.Team;
import com.teammatch.backend.entity.TeamMembership;
import com.teammatch.backend.entity.enums.MembershipStatus;
import com.teammatch.backend.entity.enums.TeamRequestStatus;
import com.teammatch.backend.exception.DuplicateResourceException;
import com.teammatch.backend.exception.ForbiddenOperationException;
import com.teammatch.backend.exception.ResourceNotFoundException;
import com.teammatch.backend.repository.ProjectRepository;
import com.teammatch.backend.repository.SkillRepository;
import com.teammatch.backend.repository.StudentRepository;
import com.teammatch.backend.repository.TeamMembershipRepository;
import com.teammatch.backend.repository.TeamRepository;
import com.teammatch.backend.repository.TeamRequestRepository;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProjectService {
	private final ProjectRepository projectRepository;
	private final SkillRepository skillRepository;
	private final StudentRepository studentRepository;
	private final TeamRepository teamRepository;
	private final TeamMembershipRepository teamMembershipRepository;
	private final TeamRequestRepository teamRequestRepository;

	public ProjectService(ProjectRepository projectRepository, SkillRepository skillRepository,
			StudentRepository studentRepository, TeamRepository teamRepository,
			TeamMembershipRepository teamMembershipRepository, TeamRequestRepository teamRequestRepository) {
		this.projectRepository = projectRepository;
		this.skillRepository = skillRepository;
		this.studentRepository = studentRepository;
		this.teamRepository = teamRepository;
		this.teamMembershipRepository = teamMembershipRepository;
		this.teamRequestRepository = teamRequestRepository;
	}

	public ProjectResponse create(ProjectCreateRequest request) {
		Student owner = studentRepository.findById(request.ownerStudentId())
				.orElseThrow(() -> new ResourceNotFoundException(
						"Student " + request.ownerStudentId() + " was not found."));
		Project project = new Project(request.title().trim(), request.description().trim(), owner,
				findRequiredSkills(request.requiredSkillIds()),
				request.requiredTeamSize(), ProjectStatus.OPEN);
		Project savedProject = projectRepository.save(project);
		String teamName = savedProject.getTitle() + " Team";
		if (teamName.length() > 100) {
			teamName = teamName.substring(0, 100);
		}
		Team team = teamRepository.save(new Team(savedProject, teamName));
		teamMembershipRepository.save(new TeamMembership(team, owner, TeamMemberRole.LEADER));
		return toResponse(savedProject);
	}

	public ProjectResponse update(Long projectId, Long authenticatedStudentId, ProjectCreateRequest request) {
		Project project = projectRepository.findByIdForUpdate(projectId)
				.orElseThrow(() -> new ResourceNotFoundException("Project " + projectId + " was not found."));
		if (authenticatedStudentId == null || !authenticatedStudentId.equals(project.getOwner().getId())
				|| !authenticatedStudentId.equals(request.ownerStudentId())) {
			throw new ForbiddenOperationException("Only the project owner can edit this project.");
		}

		Team team = teamRepository.findByProject_IdForUpdate(projectId).orElse(null);
		if (team != null) {
			long activeMembers = teamMembershipRepository.countByTeam_IdAndStatus(team.getId(), MembershipStatus.ACTIVE);
			long pendingRequests = teamRequestRepository.countByRequestedTeam_IdAndStatus(
					team.getId(), TeamRequestStatus.PENDING);
			if (activeMembers + pendingRequests > request.requiredTeamSize()) {
				throw new DuplicateResourceException(
						"The required team size cannot be lower than current members and pending requests.");
			}
		}

		Set<Skill> requiredSkills = findRequiredSkills(request.requiredSkillIds());
		project.setTitle(request.title().trim());
		project.setDescription(request.description().trim());
		project.setRequiredTeamSize(request.requiredTeamSize());
		project.setRequiredSkills(requiredSkills);
		return toResponse(projectRepository.save(project));
	}

	public ProjectResponse close(Long projectId, Long authenticatedStudentId) {
		Project project = projectRepository.findByIdForUpdate(projectId)
				.orElseThrow(() -> new ResourceNotFoundException("Project " + projectId + " was not found."));
		if (authenticatedStudentId == null || !authenticatedStudentId.equals(project.getOwner().getId())) {
			throw new ForbiddenOperationException("Only the project owner can close this project.");
		}
		if (project.getStatus() != ProjectStatus.OPEN) {
			throw new DuplicateResourceException("Only an open project can be cancelled.");
		}

		project.setStatus(ProjectStatus.CANCELLED);
		return toResponse(projectRepository.save(project));
	}

	@Transactional(readOnly = true)
	public List<ProjectResponse> findAll() {
		return projectRepository.findAll().stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public ProjectResponse findById(Long id) {
		Project project = projectRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Project " + id + " was not found."));
		return toResponse(project);
	}

	private ProjectResponse toResponse(Project project) {
		List<SkillResponse> requiredSkills = project.getRequiredSkills().stream()
				.sorted(Comparator.comparing(Skill::getId))
				.map(skill -> new SkillResponse(skill.getId(), skill.getName()))
				.toList();
		Team team = project.getId() == null ? null : teamRepository.findByProject_Id(project.getId()).orElse(null);
		List<TeamMembership> memberships = team == null ? List.of()
				: teamMembershipRepository.findAllByTeam_IdAndStatus(team.getId(), MembershipStatus.ACTIVE);
		String leaderName = memberships.stream()
				.filter(membership -> membership.getRole() == TeamMemberRole.LEADER)
				.map(membership -> membership.getStudent().getName())
				.findFirst()
				.orElse(project.getOwner().getName());
		List<ProjectTeamMemberResponse> members = memberships.stream()
				.map(membership -> new ProjectTeamMemberResponse(membership.getStudent().getId(),
						membership.getStudent().getName(), membership.getRole()))
				.toList();
		return new ProjectResponse(project.getId(), project.getTitle(), project.getDescription(),
				project.getOwner().getId(), project.getOwner().getName(), project.getRequiredTeamSize(),
				project.getStatus(), requiredSkills, team == null ? null : team.getTeamName(), leaderName, members);
	}

	private Set<Skill> findRequiredSkills(List<Long> requiredSkillIds) {
		Set<Long> requestedSkillIds = new LinkedHashSet<>(requiredSkillIds);
		List<Skill> foundSkills = skillRepository.findAllById(requestedSkillIds);
		Map<Long, Skill> skillsById = foundSkills.stream()
				.collect(Collectors.toMap(Skill::getId, Function.identity()));
		for (Long skillId : requestedSkillIds) {
			if (!skillsById.containsKey(skillId)) {
				throw new ResourceNotFoundException("Skill " + skillId + " was not found.");
			}
		}
		Set<Skill> requiredSkills = new LinkedHashSet<>();
		requestedSkillIds.forEach(skillId -> requiredSkills.add(skillsById.get(skillId)));
		return requiredSkills;
	}
}
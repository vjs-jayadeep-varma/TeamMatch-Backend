package com.teammatch.backend.service;

import com.teammatch.backend.dto.StudentRecommendationResponse;
import com.teammatch.backend.entity.Project;
import com.teammatch.backend.entity.Student;
import com.teammatch.backend.entity.StudentSkill;
import com.teammatch.backend.entity.TeamMembership;
import com.teammatch.backend.entity.TeamRequest;
import com.teammatch.backend.entity.enums.MembershipStatus;
import com.teammatch.backend.entity.enums.ProjectStatus;
import com.teammatch.backend.entity.enums.TeamRequestStatus;
import com.teammatch.backend.exception.DuplicateResourceException;
import com.teammatch.backend.exception.ForbiddenOperationException;
import com.teammatch.backend.exception.ResourceNotFoundException;
import com.teammatch.backend.repository.ProjectRepository;
import com.teammatch.backend.repository.StudentRepository;
import com.teammatch.backend.repository.StudentSkillRepository;
import com.teammatch.backend.repository.TeamMembershipRepository;
import com.teammatch.backend.repository.TeamRepository;
import com.teammatch.backend.repository.TeamRequestRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MatchingService {
	private final ProjectRepository projectRepository;
	private final StudentRepository studentRepository;
	private final StudentSkillRepository studentSkillRepository;
	private final TeamRepository teamRepository;
	private final TeamMembershipRepository teamMembershipRepository;
	private final TeamRequestRepository teamRequestRepository;

	public MatchingService(ProjectRepository projectRepository, StudentRepository studentRepository,
			StudentSkillRepository studentSkillRepository, TeamRepository teamRepository,
			TeamMembershipRepository teamMembershipRepository, TeamRequestRepository teamRequestRepository) {
		this.projectRepository = projectRepository;
		this.studentRepository = studentRepository;
		this.studentSkillRepository = studentSkillRepository;
		this.teamRepository = teamRepository;
		this.teamMembershipRepository = teamMembershipRepository;
		this.teamRequestRepository = teamRequestRepository;
	}

	List<StudentRecommendationResponse> recommendForProject(Long projectId) {
		Project project = projectRepository.findById(projectId)
				.orElseThrow(() -> new ResourceNotFoundException("Project " + projectId + " was not found."));
		return recommendForProject(projectId, project, null);
	}

	public List<StudentRecommendationResponse> recommendForProject(Long projectId, Long requestingStudentId) {
		Project project = projectRepository.findById(projectId)
				.orElseThrow(() -> new ResourceNotFoundException("Project " + projectId + " was not found."));
		if (requestingStudentId == null) {
			throw new ForbiddenOperationException("A student account is required to view teammate recommendations.");
		}
		if (project.getStatus() != ProjectStatus.OPEN && project.getStatus() != ProjectStatus.IN_PROGRESS) {
			throw new DuplicateResourceException("Recommendations are not available for this project.");
		}
		return recommendForProject(projectId, project, requestingStudentId);
	}

	private List<StudentRecommendationResponse> recommendForProject(
			Long projectId, Project project, Long requestingStudentId) {
		Set<Long> requiredSkillIds = project.getRequiredSkills().stream()
				.map(skill -> skill.getId())
				.collect(Collectors.toSet());
		Map<Long, String> requiredSkillNames = new HashMap<>();
		project.getRequiredSkills().forEach(skill -> requiredSkillNames.put(skill.getId(), skill.getName()));

		Map<Long, Set<Long>> skillsByStudent = new HashMap<>();
		for (StudentSkill studentSkill : studentSkillRepository.findAll()) {
			skillsByStudent.computeIfAbsent(studentSkill.getStudent().getId(), ignored -> new HashSet<>())
					.add(studentSkill.getSkill().getId());
		}

		Set<Long> existingMemberIds = new HashSet<>();
		Set<Long> activeRequestCandidateIds = new HashSet<>();
		var team = teamRepository.findByProject_Id(projectId);
		if (team.isPresent()) {
			List<TeamMembership> activeMemberships = teamMembershipRepository.findAllByTeam_IdAndStatus(
					team.get().getId(), MembershipStatus.ACTIVE);
			activeMemberships.forEach(membership -> existingMemberIds.add(membership.getStudent().getId()));
			List<TeamRequest> requests = teamRequestRepository.findAllByRequestedTeam_IdOrderByCreatedAtDesc(
					team.get().getId());
			requests.stream()
					.filter(request -> request.getStatus() == TeamRequestStatus.PENDING
							|| request.getStatus() == TeamRequestStatus.ACCEPTED)
					.map(request -> request.getRequestingStudent().getId().equals(project.getOwner().getId())
							? request.getRecipientStudent().getId() : request.getRequestingStudent().getId())
					.forEach(activeRequestCandidateIds::add);
			long pendingRequests = requests.stream()
					.filter(request -> request.getStatus() == TeamRequestStatus.PENDING)
					.count();
			if (activeMemberships.size() + pendingRequests >= project.getRequiredTeamSize()) {
				return List.of();
			}
		}

		List<StudentRecommendationResponse> recommendations = new ArrayList<>();
		for (Student student : studentRepository.findAll()) {
			if (!student.isAvailable() || student.getId().equals(project.getOwner().getId())
					|| (requestingStudentId != null && student.getId().equals(requestingStudentId))
					|| existingMemberIds.contains(student.getId())
					|| activeRequestCandidateIds.contains(student.getId())) {
				continue;
			}

			Set<Long> studentSkillIds = skillsByStudent.getOrDefault(student.getId(), Set.of());
			List<String> matchedSkills = requiredSkillNames.entrySet().stream()
					.filter(entry -> studentSkillIds.contains(entry.getKey()))
					.map(Map.Entry::getValue)
					.sorted(String.CASE_INSENSITIVE_ORDER)
					.toList();
			List<String> missingSkills = requiredSkillNames.entrySet().stream()
					.filter(entry -> !studentSkillIds.contains(entry.getKey()))
					.map(Map.Entry::getValue)
					.sorted(String.CASE_INSENSITIVE_ORDER)
					.toList();
			double score = requiredSkillIds.isEmpty()
					? 100.0
					: matchedSkills.size() * 100.0 / requiredSkillIds.size();
			recommendations.add(new StudentRecommendationResponse(student.getId(), student.getName(), student.getEmail(),
					student.getDepartment(), student.getAcademicYear(), score, matchedSkills, missingSkills));
		}

		return recommendations.stream()
				.sorted(Comparator.comparingDouble(StudentRecommendationResponse::compatibilityScore).reversed()
						.thenComparing(StudentRecommendationResponse::name, String.CASE_INSENSITIVE_ORDER)
						.thenComparing(StudentRecommendationResponse::studentId))
				.toList();
	}
}
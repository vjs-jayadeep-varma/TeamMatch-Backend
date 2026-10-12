package com.teammatch.backend.service;

import com.teammatch.backend.dto.StudentTeamResponse;
import com.teammatch.backend.dto.TeamMemberResponse;
import com.teammatch.backend.dto.TeamProjectResponse;
import com.teammatch.backend.entity.Team;
import com.teammatch.backend.entity.TeamMembership;
import com.teammatch.backend.entity.enums.MembershipStatus;
import com.teammatch.backend.exception.ForbiddenOperationException;
import com.teammatch.backend.exception.ResourceNotFoundException;
import com.teammatch.backend.repository.StudentRepository;
import com.teammatch.backend.repository.TeamMembershipRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StudentTeamService {
	private final StudentRepository studentRepository;
	private final TeamMembershipRepository teamMembershipRepository;

	public StudentTeamService(StudentRepository studentRepository,
			TeamMembershipRepository teamMembershipRepository) {
		this.studentRepository = studentRepository;
		this.teamMembershipRepository = teamMembershipRepository;
	}

	public List<StudentTeamResponse> findTeams(Long authenticatedStudentId, Long requestedStudentId) {
		if (authenticatedStudentId == null || !authenticatedStudentId.equals(requestedStudentId)) {
			throw new ForbiddenOperationException("You can only view teams for your own student account.");
		}
		if (!studentRepository.existsById(authenticatedStudentId)) {
			throw new ResourceNotFoundException("Student " + authenticatedStudentId + " was not found.");
		}

		return teamMembershipRepository.findAllByStudent_IdAndStatusOrderByTeam_IdAsc(
				authenticatedStudentId, MembershipStatus.ACTIVE).stream()
				.map(this::toResponse)
				.toList();
	}

	private StudentTeamResponse toResponse(TeamMembership studentMembership) {
		Team team = studentMembership.getTeam();
		var project = team.getProject();
		List<TeamMemberResponse> members = teamMembershipRepository
				.findAllByTeam_IdAndStatus(team.getId(), MembershipStatus.ACTIVE).stream()
				.map(this::toMemberResponse)
				.toList();

		return new StudentTeamResponse(team.getId(), team.getTeamName(), team.getStatus(),
				studentMembership.getRole(), studentMembership.getStatus(),
				new TeamProjectResponse(project.getId(), project.getTitle(), project.getDescription(),
						project.getOwner().getId(), project.getOwner().getName(),
						project.getRequiredTeamSize(), project.getStatus()),
				members);
	}

	private TeamMemberResponse toMemberResponse(TeamMembership membership) {
		var student = membership.getStudent();
		return new TeamMemberResponse(student.getId(), student.getName(), student.getEmail(),
				student.getDepartment(), student.getAcademicYear(), membership.getRole(), membership.getStatus());
	}
}

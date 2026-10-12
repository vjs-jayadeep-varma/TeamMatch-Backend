package com.teammatch.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.teammatch.backend.dto.StudentTeamResponse;
import com.teammatch.backend.entity.Project;
import com.teammatch.backend.entity.Student;
import com.teammatch.backend.entity.Team;
import com.teammatch.backend.entity.TeamMembership;
import com.teammatch.backend.entity.enums.MembershipStatus;
import com.teammatch.backend.entity.enums.ProjectStatus;
import com.teammatch.backend.entity.enums.TeamMemberRole;
import com.teammatch.backend.entity.enums.TeamStatus;
import com.teammatch.backend.exception.ForbiddenOperationException;
import com.teammatch.backend.repository.StudentRepository;
import com.teammatch.backend.repository.TeamMembershipRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudentTeamServiceTest {
	@Mock
	private StudentRepository studentRepository;

	@Mock
	private TeamMembershipRepository teamMembershipRepository;

	@InjectMocks
	private StudentTeamService studentTeamService;

	@Test
	void returnsActiveTeamsWithProjectAndActiveMembers() {
		Student student = student(5L, "Student");
		Student leader = student(1L, "Leader");
		Project project = project();
		Team team = team(project);
		TeamMembership studentMembership = membership(team, student, TeamMemberRole.MEMBER, MembershipStatus.ACTIVE);
		TeamMembership leaderMembership = mock(TeamMembership.class);
		when(leaderMembership.getStudent()).thenReturn(leader);
		when(leaderMembership.getRole()).thenReturn(TeamMemberRole.LEADER);
		when(leaderMembership.getStatus()).thenReturn(MembershipStatus.ACTIVE);
		when(studentRepository.existsById(5L)).thenReturn(true);
		when(teamMembershipRepository.findAllByStudent_IdAndStatusOrderByTeam_IdAsc(5L, MembershipStatus.ACTIVE))
				.thenReturn(List.of(studentMembership));
		when(teamMembershipRepository.findAllByTeam_IdAndStatus(20L, MembershipStatus.ACTIVE))
				.thenReturn(List.of(leaderMembership, studentMembership));

		List<StudentTeamResponse> teams = studentTeamService.findTeams(5L, 5L);

		assertEquals(1, teams.size());
		assertEquals(20L, teams.get(0).teamId());
		assertEquals(10L, teams.get(0).project().id());
		assertEquals("Team Project", teams.get(0).project().title());
		assertEquals(TeamMemberRole.MEMBER, teams.get(0).studentRole());
		assertEquals(2, teams.get(0).members().size());
		assertEquals(List.of(1L, 5L), teams.get(0).members().stream().map(member -> member.studentId()).sorted().toList());
		assertEquals(MembershipStatus.ACTIVE, teams.get(0).members().get(0).membershipStatus());
	}

	@Test
	void doesNotReturnInactiveMemberships() {
		when(studentRepository.existsById(5L)).thenReturn(true);
		when(teamMembershipRepository.findAllByStudent_IdAndStatusOrderByTeam_IdAsc(5L, MembershipStatus.ACTIVE))
				.thenReturn(List.of());

		assertEquals(List.of(), studentTeamService.findTeams(5L, 5L));
		verify(teamMembershipRepository).findAllByStudent_IdAndStatusOrderByTeam_IdAsc(5L, MembershipStatus.ACTIVE);
	}

	@Test
	void rejectsAccessToAnotherStudentsTeamsBeforeRepositoryLookup() {
		assertThrows(ForbiddenOperationException.class, () -> studentTeamService.findTeams(5L, 6L));
		verifyNoInteractions(teamMembershipRepository);
	}

	private Student student(Long id, String name) {
		Student student = mock(Student.class);
		when(student.getId()).thenReturn(id);
		when(student.getName()).thenReturn(name);
		when(student.getEmail()).thenReturn(name.toLowerCase() + "@example.com");
		when(student.getDepartment()).thenReturn("CSE");
		when(student.getAcademicYear()).thenReturn(2);
		return student;
	}

	private Project project() {
		Project project = mock(Project.class);
		Student owner = mock(Student.class);
		when(owner.getId()).thenReturn(1L);
		when(project.getId()).thenReturn(10L);
		when(project.getTitle()).thenReturn("Team Project");
		when(project.getDescription()).thenReturn("Project description");
		when(project.getOwner()).thenReturn(owner);
		when(project.getRequiredTeamSize()).thenReturn(4);
		when(project.getStatus()).thenReturn(ProjectStatus.IN_PROGRESS);
		return project;
	}

	private Team team(Project project) {
		Team team = mock(Team.class);
		when(team.getId()).thenReturn(20L);
		when(team.getTeamName()).thenReturn("Team Project Team");
		when(team.getStatus()).thenReturn(TeamStatus.FORMING);
		when(team.getProject()).thenReturn(project);
		return team;
	}

	private TeamMembership membership(Team team, Student student, TeamMemberRole role, MembershipStatus status) {
		TeamMembership membership = mock(TeamMembership.class);
		when(membership.getTeam()).thenReturn(team);
		when(membership.getStudent()).thenReturn(student);
		when(membership.getRole()).thenReturn(role);
		when(membership.getStatus()).thenReturn(status);
		return membership;
	}
}

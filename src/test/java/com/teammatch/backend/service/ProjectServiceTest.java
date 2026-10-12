package com.teammatch.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teammatch.backend.dto.ProjectCreateRequest;
import com.teammatch.backend.dto.ProjectResponse;
import com.teammatch.backend.entity.Project;
import com.teammatch.backend.entity.Skill;
import com.teammatch.backend.entity.Student;
import com.teammatch.backend.entity.Team;
import com.teammatch.backend.entity.TeamMembership;
import com.teammatch.backend.entity.enums.MembershipStatus;
import com.teammatch.backend.entity.enums.ProjectStatus;
import com.teammatch.backend.entity.enums.TeamMemberRole;
import com.teammatch.backend.entity.enums.TeamRequestStatus;
import com.teammatch.backend.exception.DuplicateResourceException;
import com.teammatch.backend.exception.ResourceNotFoundException;
import com.teammatch.backend.exception.ForbiddenOperationException;
import com.teammatch.backend.repository.ProjectRepository;
import com.teammatch.backend.repository.SkillRepository;
import com.teammatch.backend.repository.StudentRepository;
import com.teammatch.backend.repository.TeamMembershipRepository;
import com.teammatch.backend.repository.TeamRepository;
import com.teammatch.backend.repository.TeamRequestRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {
	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private SkillRepository skillRepository;

	@Mock
	private StudentRepository studentRepository;

	@Mock
	private TeamRepository teamRepository;

	@Mock
	private TeamMembershipRepository teamMembershipRepository;

	@Mock
	private TeamRequestRepository teamRequestRepository;

	@InjectMocks
	private ProjectService projectService;

	@Test
	void createAssociatesOwnerAndRequiredSkillsAndOpensProject() {
		Student owner = org.mockito.Mockito.mock(Student.class);
		Skill skill = org.mockito.Mockito.mock(Skill.class);
		when(owner.getId()).thenReturn(1L);
		when(skill.getId()).thenReturn(2L);
		when(skill.getName()).thenReturn("Java");
		when(studentRepository.findById(1L)).thenReturn(Optional.of(owner));
		when(skillRepository.findAllById(any(Iterable.class))).thenReturn(List.of(skill));
		when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ProjectResponse response = projectService.create(request());

		ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
		verify(projectRepository).save(projectCaptor.capture());
		Project saved = projectCaptor.getValue();
		assertEquals(owner, saved.getOwner());
		assertEquals(1, saved.getRequiredSkills().size());
		assertEquals(ProjectStatus.OPEN, response.status());
		assertEquals("Project1", response.title());
		assertEquals("Java", response.requiredSkills().get(0).name());
		ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);
		verify(teamRepository).save(teamCaptor.capture());
		assertEquals(saved, teamCaptor.getValue().getProject());
		assertEquals("Project1 Team", teamCaptor.getValue().getTeamName());
		ArgumentCaptor<TeamMembership> membershipCaptor = ArgumentCaptor.forClass(TeamMembership.class);
		verify(teamMembershipRepository).save(membershipCaptor.capture());
		assertEquals(owner, membershipCaptor.getValue().getStudent());
		assertEquals(TeamMemberRole.LEADER, membershipCaptor.getValue().getRole());
		assertEquals(teamCaptor.getValue(), membershipCaptor.getValue().getTeam());
	}

	@Test
	void findAllReturnsEveryProject() {
		when(projectRepository.findAll()).thenReturn(List.of());

		assertEquals(List.of(), projectService.findAll());
		verify(projectRepository).findAll();
	}

	@Test
	void findByIdReturnsProjectDetails() {
		Project project = org.mockito.Mockito.mock(Project.class);
		Student owner = org.mockito.Mockito.mock(Student.class);
		when(projectRepository.findById(3L)).thenReturn(Optional.of(project));
		when(project.getOwner()).thenReturn(owner);
		when(owner.getId()).thenReturn(1L);
		when(project.getRequiredSkills()).thenReturn(Set.of());
		when(project.getTitle()).thenReturn("Project1");
		when(project.getDescription()).thenReturn("Demo project");
		when(project.getRequiredTeamSize()).thenReturn(4);
		when(project.getStatus()).thenReturn(ProjectStatus.OPEN);

		ProjectResponse response = projectService.findById(3L);
		assertEquals("Project1", response.title());
		assertEquals(1L, response.ownerStudentId());
		verify(projectRepository).findById(3L);
	}

	@Test
	void sharedProjectDetailsIncludeLeaderAndCurrentMemberNames() {
		Project project = org.mockito.Mockito.mock(Project.class);
		Student owner = org.mockito.Mockito.mock(Student.class);
		Student memberStudent = org.mockito.Mockito.mock(Student.class);
		Team team = org.mockito.Mockito.mock(Team.class);
		TeamMembership leaderMembership = org.mockito.Mockito.mock(TeamMembership.class);
		TeamMembership memberMembership = org.mockito.Mockito.mock(TeamMembership.class);
		when(projectRepository.findById(3L)).thenReturn(Optional.of(project));
		when(project.getId()).thenReturn(3L);
		when(project.getOwner()).thenReturn(owner);
		when(project.getRequiredSkills()).thenReturn(Set.of());
		when(project.getTitle()).thenReturn("Project1");
		when(project.getDescription()).thenReturn("Demo project");
		when(project.getRequiredTeamSize()).thenReturn(4);
		when(project.getStatus()).thenReturn(ProjectStatus.OPEN);
		when(owner.getId()).thenReturn(1L);
		when(owner.getName()).thenReturn("Leader");
		when(teamRepository.findByProject_Id(3L)).thenReturn(Optional.of(team));
		when(team.getId()).thenReturn(30L);
		when(team.getTeamName()).thenReturn("Project1 Team");
		when(teamMembershipRepository.findAllByTeam_IdAndStatus(30L, MembershipStatus.ACTIVE))
				.thenReturn(List.of(leaderMembership, memberMembership));
		when(leaderMembership.getStudent()).thenReturn(owner);
		when(leaderMembership.getRole()).thenReturn(TeamMemberRole.LEADER);
		when(memberMembership.getStudent()).thenReturn(memberStudent);
		when(memberMembership.getRole()).thenReturn(TeamMemberRole.MEMBER);
		when(memberStudent.getId()).thenReturn(2L);
		when(memberStudent.getName()).thenReturn("Member");

		ProjectResponse response = projectService.findById(3L);

		assertEquals("Leader", response.teamLeaderName());
		assertEquals("Project1 Team", response.teamName());
		assertEquals(List.of("Leader", "Member"),
				response.teamMembers().stream().map(member -> member.name()).toList());
	}

	@Test
	void createRejectsMissingOwner() {
		when(studentRepository.findById(1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> projectService.create(request()));
		verify(projectRepository, never()).save(any(Project.class));
	}

	@Test
	void createRejectsMissingRequiredSkill() {
		Student owner = org.mockito.Mockito.mock(Student.class);
		when(studentRepository.findById(1L)).thenReturn(Optional.of(owner));
		when(skillRepository.findAllById(any(Iterable.class))).thenReturn(List.of());

		assertThrows(ResourceNotFoundException.class, () -> projectService.create(request()));
		verify(projectRepository, never()).save(any(Project.class));
	}

	@Test
	void updateEditsOnlyTheAuthenticatedOwnerProject() {
		Project project = org.mockito.Mockito.mock(Project.class);
		Student owner = org.mockito.Mockito.mock(Student.class);
		Skill skill = org.mockito.Mockito.mock(Skill.class);
		when(projectRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(project));
		when(project.getOwner()).thenReturn(owner);
		when(owner.getId()).thenReturn(1L);
		when(teamRepository.findByProject_IdForUpdate(3L)).thenReturn(Optional.empty());
		when(skillRepository.findAllById(any(Iterable.class))).thenReturn(List.of(skill));
		when(skill.getId()).thenReturn(2L);
		when(skill.getName()).thenReturn("Java");
		when(projectRepository.save(project)).thenReturn(project);
		when(project.getRequiredSkills()).thenReturn(Set.of(skill));
		when(project.getTitle()).thenReturn("Updated project");
		when(project.getDescription()).thenReturn("Updated description");
		when(project.getRequiredTeamSize()).thenReturn(4);
		when(project.getStatus()).thenReturn(ProjectStatus.OPEN);

		ProjectResponse response = projectService.update(3L, 1L,
				new ProjectCreateRequest(" Updated project ", " Updated description ", 1L, 4, List.of(2L)));

		assertEquals("Updated project", response.title());
		assertEquals("Updated description", response.description());
		verify(project).setTitle("Updated project");
		verify(project).setDescription("Updated description");
		verify(project).setRequiredTeamSize(4);
		verify(project).setRequiredSkills(Set.of(skill));
	}

	@Test
	void ownerCanCloseOpenProject() {
		Project project = org.mockito.Mockito.mock(Project.class);
		Student owner = org.mockito.Mockito.mock(Student.class);
		when(projectRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(project));
		when(project.getId()).thenReturn(3L);
		when(project.getOwner()).thenReturn(owner);
		when(owner.getId()).thenReturn(1L);
		when(owner.getName()).thenReturn("Owner");
		when(project.getRequiredSkills()).thenReturn(Set.of());
		when(project.getTitle()).thenReturn("Project1");
		when(project.getDescription()).thenReturn("Demo project");
		when(project.getRequiredTeamSize()).thenReturn(4);
		when(project.getStatus()).thenReturn(ProjectStatus.OPEN, ProjectStatus.CANCELLED);
		when(projectRepository.save(project)).thenReturn(project);

		ProjectResponse response = projectService.close(3L, 1L);

		assertEquals(ProjectStatus.CANCELLED, response.status());
		verify(project).setStatus(ProjectStatus.CANCELLED);
		verify(projectRepository).save(project);
	}

	@Test
	void nonOwnerCannotCloseProject() {
		Project project = org.mockito.Mockito.mock(Project.class);
		Student owner = org.mockito.Mockito.mock(Student.class);
		when(projectRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(project));
		when(project.getOwner()).thenReturn(owner);
		when(owner.getId()).thenReturn(1L);

		assertThrows(ForbiddenOperationException.class, () -> projectService.close(3L, 2L));
		verify(projectRepository, never()).save(any(Project.class));
	}

	@Test
	void onlyOpenProjectsCanBeCancelled() {
		Project project = org.mockito.Mockito.mock(Project.class);
		Student owner = org.mockito.Mockito.mock(Student.class);
		when(projectRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(project));
		when(project.getOwner()).thenReturn(owner);
		when(owner.getId()).thenReturn(1L);
		when(project.getStatus()).thenReturn(ProjectStatus.IN_PROGRESS);

		assertThrows(DuplicateResourceException.class, () -> projectService.close(3L, 1L));
		verify(projectRepository, never()).save(any(Project.class));
	}

	@Test
	void cancelledProjectCannotBeReopenedByClosingItAgain() {
		Project project = org.mockito.Mockito.mock(Project.class);
		Student owner = org.mockito.Mockito.mock(Student.class);
		when(projectRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(project));
		when(project.getOwner()).thenReturn(owner);
		when(owner.getId()).thenReturn(1L);
		when(project.getStatus()).thenReturn(ProjectStatus.CANCELLED);

		assertThrows(DuplicateResourceException.class, () -> projectService.close(3L, 1L));
		verify(projectRepository, never()).save(any(Project.class));
	}

	@Test
	void updateRejectsAnotherStudentsProject() {
		Project project = org.mockito.Mockito.mock(Project.class);
		Student owner = org.mockito.Mockito.mock(Student.class);
		when(projectRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(project));
		when(project.getOwner()).thenReturn(owner);
		when(owner.getId()).thenReturn(1L);

		assertThrows(ForbiddenOperationException.class, () -> projectService.update(3L, 2L,
				new ProjectCreateRequest("Updated", "Description", 2L, 4, List.of())));
		verify(projectRepository, never()).save(any(Project.class));
	}

	@Test
	void updateCannotReduceTeamSizeBelowActiveMembersAndPendingRequests() {
		Project project = org.mockito.Mockito.mock(Project.class);
		Student owner = org.mockito.Mockito.mock(Student.class);
		Team team = org.mockito.Mockito.mock(Team.class);
		when(projectRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(project));
		when(project.getOwner()).thenReturn(owner);
		when(owner.getId()).thenReturn(1L);
		when(teamRepository.findByProject_IdForUpdate(3L)).thenReturn(Optional.of(team));
		when(team.getId()).thenReturn(4L);
		when(teamMembershipRepository.countByTeam_IdAndStatus(4L, MembershipStatus.ACTIVE)).thenReturn(2L);
		when(teamRequestRepository.countByRequestedTeam_IdAndStatus(4L, TeamRequestStatus.PENDING)).thenReturn(1L);

		assertThrows(DuplicateResourceException.class, () -> projectService.update(3L, 1L,
				new ProjectCreateRequest("Updated", "Description", 1L, 2, List.of())));
		verify(projectRepository, never()).save(any(Project.class));
	}

	private ProjectCreateRequest request() {
		return new ProjectCreateRequest(" Project1 ", " Build a demo ", 1L, 4, List.of(2L));
	}
}
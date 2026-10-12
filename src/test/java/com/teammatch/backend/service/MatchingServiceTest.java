package com.teammatch.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teammatch.backend.dto.StudentRecommendationResponse;
import com.teammatch.backend.entity.Project;
import com.teammatch.backend.entity.Skill;
import com.teammatch.backend.entity.Student;
import com.teammatch.backend.entity.StudentSkill;
import com.teammatch.backend.entity.Team;
import com.teammatch.backend.entity.TeamMembership;
import com.teammatch.backend.entity.TeamRequest;
import com.teammatch.backend.entity.enums.MembershipStatus;
import com.teammatch.backend.entity.enums.ProjectStatus;
import com.teammatch.backend.entity.enums.TeamMemberRole;
import com.teammatch.backend.entity.enums.TeamRequestStatus;
import com.teammatch.backend.exception.DuplicateResourceException;
import com.teammatch.backend.exception.ResourceNotFoundException;
import com.teammatch.backend.repository.ProjectRepository;
import com.teammatch.backend.repository.StudentRepository;
import com.teammatch.backend.repository.StudentSkillRepository;
import com.teammatch.backend.repository.TeamMembershipRepository;
import com.teammatch.backend.repository.TeamRepository;
import com.teammatch.backend.repository.TeamRequestRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {
	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private StudentRepository studentRepository;

	@Mock
	private StudentSkillRepository studentSkillRepository;

	@Mock
	private TeamRepository teamRepository;

	@Mock
	private TeamMembershipRepository teamMembershipRepository;

	@Mock
	private TeamRequestRepository teamRequestRepository;

	@InjectMocks
	private MatchingService matchingService;

	@Test
	void ranksStudentsByRequiredSkillCoverage() {
		Student owner = student(1L, "Student1");
		Project project = project(10L, Set.of(skill(1L, "Java"), skill(2L, "React")), owner);
		Student fullMatch = student(2L, "Student2");
		Student partialMatch = student(3L, "Student3");
		StudentSkill fullMatchJava = studentSkill(2L, 1L);
		StudentSkill fullMatchReact = studentSkill(2L, 2L);
		StudentSkill partialMatchJava = studentSkill(3L, 1L);
		when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
		when(teamRepository.findByProject_Id(10L)).thenReturn(Optional.empty());
		when(studentSkillRepository.findAll()).thenReturn(List.of(fullMatchJava, fullMatchReact, partialMatchJava));
		when(studentRepository.findAll()).thenReturn(List.of(owner, fullMatch, partialMatch));

		List<StudentRecommendationResponse> results = matchingService.recommendForProject(10L);

		assertEquals(List.of(2L, 3L), results.stream().map(StudentRecommendationResponse::studentId).toList());
		assertEquals(100.0, results.get(0).compatibilityScore());
		assertEquals(50.0, results.get(1).compatibilityScore());
	}

	@Test
	void noRequiredSkillsScoresAvailableCandidatesAtOneHundred() {
		Student owner = student(1L, "Student1");
		Student secondStudent = student(2L, "Student2");
		Project project = project(10L, Set.of(), owner);
		when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
		when(teamRepository.findByProject_Id(10L)).thenReturn(Optional.empty());
		when(studentSkillRepository.findAll()).thenReturn(List.of());
		when(studentRepository.findAll()).thenReturn(List.of(owner, secondStudent));

		List<StudentRecommendationResponse> results = matchingService.recommendForProject(10L);

		assertEquals(1, results.size());
		assertEquals(100.0, results.get(0).compatibilityScore());
	}

	@Test
	void missingProjectIsNotFound() {
		when(projectRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> matchingService.recommendForProject(99L));
	}

	@Test
	void anyStudentCanReadRecommendationsForAnEligibleSharedProject() {
		Student owner = student(1L, "Owner");
		Project project = project(10L, Set.of(), owner);
		when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
		when(project.getStatus()).thenReturn(ProjectStatus.OPEN);
		Student other = student(2L, "Other");
		Student candidate = student(3L, "Candidate");
		when(teamRepository.findByProject_Id(10L)).thenReturn(Optional.empty());
		when(studentSkillRepository.findAll()).thenReturn(List.of());
		when(studentRepository.findAll()).thenReturn(List.of(owner, other, candidate));

		List<StudentRecommendationResponse> results = matchingService.recommendForProject(10L, 2L);

		verify(projectRepository).findById(10L);
		assertEquals(List.of(3L), results.stream().map(StudentRecommendationResponse::studentId).toList());
	}

	@Test
	void recommendationsExcludeStudentsWithPendingRequests() {
		Student owner = student(1L, "Owner");
		Student pendingCandidate = student(2L, "Pending");
		Student freeCandidate = student(3L, "Free");
		Project project = project(10L, Set.of(), owner);
		when(project.getRequiredTeamSize()).thenReturn(4);
		Team team = mock(Team.class);
		TeamRequest pendingRequest = new TeamRequest(owner, pendingCandidate, team);
		pendingRequest.setStatus(TeamRequestStatus.PENDING);
		when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
		when(teamRepository.findByProject_Id(10L)).thenReturn(Optional.of(team));
		when(team.getId()).thenReturn(20L);
		when(teamMembershipRepository.findAllByTeam_IdAndStatus(20L, MembershipStatus.ACTIVE))
				.thenReturn(List.of(new TeamMembership(team, owner, TeamMemberRole.LEADER)));
		when(teamRequestRepository.findAllByRequestedTeam_IdOrderByCreatedAtDesc(20L))
				.thenReturn(List.of(pendingRequest));
		when(studentSkillRepository.findAll()).thenReturn(List.of());
		when(studentRepository.findAll()).thenReturn(List.of(owner, pendingCandidate, freeCandidate));

		List<StudentRecommendationResponse> results = matchingService.recommendForProject(10L, 1L);

		assertEquals(List.of(3L), results.stream().map(StudentRecommendationResponse::studentId).toList());
	}

	@Test
	void recommendationsAreEmptyWhenActiveMembersAndPendingRequestsFillCapacity() {
		Student owner = student(1L, "Owner");
		Project project = project(10L, Set.of(), owner);
		org.mockito.Mockito.when(project.getRequiredTeamSize()).thenReturn(2);
		Team team = mock(Team.class);
		Student member = student(2L, "Member");
		Student pending = student(3L, "Pending");
		TeamRequest pendingRequest = new TeamRequest(owner, pending, team);
		pendingRequest.setStatus(TeamRequestStatus.PENDING);
		when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
		when(teamRepository.findByProject_Id(10L)).thenReturn(Optional.of(team));
		when(team.getId()).thenReturn(20L);
		when(teamMembershipRepository.findAllByTeam_IdAndStatus(20L, MembershipStatus.ACTIVE))
				.thenReturn(List.of(new TeamMembership(team, owner, TeamMemberRole.LEADER)));
		when(teamRequestRepository.findAllByRequestedTeam_IdOrderByCreatedAtDesc(20L))
				.thenReturn(List.of(pendingRequest));
		when(studentSkillRepository.findAll()).thenReturn(List.of());

		assertEquals(List.of(), matchingService.recommendForProject(10L, 4L));
		org.mockito.Mockito.verifyNoInteractions(studentRepository);
	}

	@Test
	void recommendationsReturnEmptyForEligibleProjectWithoutAvailableCandidates() {
		Student owner = student(1L, "Owner");
		Student unavailable = student(2L, "Unavailable");
		when(unavailable.isAvailable()).thenReturn(false);
		Project project = project(10L, Set.of(skill(1L, "Java")), owner);
		when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
		when(teamRepository.findByProject_Id(10L)).thenReturn(Optional.empty());
		when(studentSkillRepository.findAll()).thenReturn(List.of());
		when(studentRepository.findAll()).thenReturn(List.of(owner, unavailable));

		assertEquals(List.of(), matchingService.recommendForProject(10L, 1L));
	}

	@Test
	void recommendationsRejectIneligibleProjectStatus() {
		Project project = project(10L, Set.of(), student(1L, "Owner"));
		when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
		when(project.getStatus()).thenReturn(ProjectStatus.COMPLETED);

		assertThrows(DuplicateResourceException.class, () -> matchingService.recommendForProject(10L, 2L));
		org.mockito.Mockito.verifyNoInteractions(studentRepository, studentSkillRepository, teamRepository,
				teamMembershipRepository);
	}

	private Project project(Long id, Set<Skill> skills, Student owner) {
		Project project = mock(Project.class);
		lenient().when(project.getId()).thenReturn(id);
		lenient().when(project.getRequiredSkills()).thenReturn(skills);
		lenient().when(project.getOwner()).thenReturn(owner);
		lenient().when(project.getStatus()).thenReturn(ProjectStatus.OPEN);
		return project;
	}

	private Student student(Long id, String name) {
		Student student = mock(Student.class);
		lenient().when(student.getId()).thenReturn(id);
		lenient().when(student.getName()).thenReturn(name);
		lenient().when(student.getEmail()).thenReturn(name.toLowerCase() + "@example.com");
		lenient().when(student.getDepartment()).thenReturn("CSE");
		lenient().when(student.getAcademicYear()).thenReturn(2);
		lenient().when(student.isAvailable()).thenReturn(true);
		return student;
	}

	private Skill skill(Long id, String name) {
		Skill skill = mock(Skill.class);
		lenient().when(skill.getId()).thenReturn(id);
		lenient().when(skill.getName()).thenReturn(name);
		return skill;
	}

	private StudentSkill studentSkill(Long studentId, Long skillId) {
		StudentSkill studentSkill = mock(StudentSkill.class);
		Student student = mock(Student.class);
		Skill skill = mock(Skill.class);
		lenient().when(student.getId()).thenReturn(studentId);
		lenient().when(skill.getId()).thenReturn(skillId);
		lenient().when(studentSkill.getStudent()).thenReturn(student);
		lenient().when(studentSkill.getSkill()).thenReturn(skill);
		return studentSkill;
	}
}
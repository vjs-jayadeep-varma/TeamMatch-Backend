package com.teammatch.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.teammatch.backend.dto.ProjectCreateRequest;
import com.teammatch.backend.dto.SkillRequest;
import com.teammatch.backend.dto.StudentCreateRequest;
import com.teammatch.backend.entity.Student;
import com.teammatch.backend.entity.Team;
import com.teammatch.backend.entity.TeamMembership;
import com.teammatch.backend.entity.TeamRequest;
import com.teammatch.backend.entity.enums.MembershipStatus;
import com.teammatch.backend.entity.enums.TeamMemberRole;
import com.teammatch.backend.entity.enums.TeamRequestStatus;
import com.teammatch.backend.exception.DuplicateResourceException;
import com.teammatch.backend.repository.ProjectRepository;
import com.teammatch.backend.repository.SkillRepository;
import com.teammatch.backend.repository.StudentRepository;
import com.teammatch.backend.repository.StudentSkillRepository;
import com.teammatch.backend.repository.TeamMembershipRepository;
import com.teammatch.backend.repository.TeamRepository;
import com.teammatch.backend.repository.TeamRequestRepository;
import com.teammatch.backend.service.MatchingService;
import com.teammatch.backend.service.ProjectService;
import com.teammatch.backend.service.SkillService;
import com.teammatch.backend.service.StudentInterestService;
import com.teammatch.backend.service.StudentService;
import com.teammatch.backend.service.StudentSkillService;
import com.teammatch.backend.service.StudentTeamService;
import com.teammatch.backend.service.TeamRequestService;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:demo-seeder-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"TEAMMATCH_DEV_USERNAME=test-development-account",
		"TEAMMATCH_DEV_PASSWORD=test-development-password",
		"teammatch.demo-seed.enabled=false"
})
@ActiveProfiles("dev")
class DemoDataSeederTest {
	@Autowired
	private StudentService studentService;
	@Autowired
	private StudentRepository studentRepository;
	@Autowired
	private SkillService skillService;
	@Autowired
	private StudentSkillService studentSkillService;
	@Autowired
	private StudentSkillRepository studentSkillRepository;
	@Autowired
	private SkillRepository skillRepository;
	@Autowired
	private StudentInterestService studentInterestService;
	@Autowired
	private ProjectService projectService;
	@Autowired
	private TeamRequestService teamRequestService;
	@Autowired
	private TeamRepository teamRepository;
	@Autowired
	private TeamMembershipRepository teamMembershipRepository;
	@Autowired
	private TeamRequestRepository teamRequestRepository;
	@Autowired
	private ProjectRepository projectRepository;
	@Autowired
	private MatchingService matchingService;
	@Autowired
	private StudentTeamService studentTeamService;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private PlatformTransactionManager transactionManager;
	@Autowired
	private Environment environment;

	private DemoDataSeeder seeder;

	@BeforeEach
	void createSeeder() {
		teamRequestRepository.deleteAll();
		teamMembershipRepository.deleteAll();
		teamRepository.deleteAll();
		projectRepository.deleteAll();
		studentSkillRepository.deleteAll();
		studentRepository.deleteAll();
		skillRepository.deleteAll();
		seeder = new DemoDataSeeder(studentService, studentRepository, skillService, studentSkillService,
				studentInterestService, projectService, teamRequestService, teamRepository,
				teamMembershipRepository, teamRequestRepository, transactionManager, environment);
	}

	@Test
	void seedsRealWorkflowRecordsAndCanBeRunRepeatedly() {
		DemoDataSeeder.SeedSummary firstRun = seeder.seedInTransaction();

		assertEquals(10, firstRun.studentsCreated());
		assertEquals(20, firstRun.catalogSkillsCreated());
		assertEquals(53, firstRun.studentSkillsAdded());
		assertEquals(8, firstRun.projectsCreated());
		assertEquals(8, firstRun.teamsCreated());
		assertEquals(11, firstRun.membershipsCreated());
		assertEquals(6, firstRun.requestsCreated());
		assertEquals(10, studentRepository.count());
		assertEquals(20, skillService.findAll().size());
		assertEquals(53, studentSkillRepository.count());
		assertEquals(8, projectRepository.count());
		assertEquals(8, teamRepository.count());
		assertEquals(11, teamMembershipRepository.count());
		assertEquals(6, teamRequestRepository.count());
		assertEquals(3, teamRequestRepository.findAll().stream()
				.filter(request -> request.getStatus() == TeamRequestStatus.ACCEPTED).count());
		assertEquals(2, teamRequestRepository.findAll().stream()
				.filter(request -> request.getStatus() == TeamRequestStatus.PENDING).count());
		assertEquals(1, teamRequestRepository.findAll().stream()
				.filter(request -> request.getStatus() == TeamRequestStatus.REJECTED).count());
		assertTrue(passwordEncoder.matches("student1@gmail.com",
				studentRepository.findByEmail("student1@gmail.com").orElseThrow().getPasswordHash()));
		assertEquals(2, studentInterestService.findByStudentId(
				studentRepository.findByEmail("student1@gmail.com").orElseThrow().getId()).interests().size());

		Long javaProjectId = projectService.findAll().stream()
				.filter(project -> project.title().equals("Java Application Development"))
				.findFirst().orElseThrow().id();
		Long javaOwnerId = studentRepository.findByEmail("student1@gmail.com").orElseThrow().getId();
		assertTrue(matchingService.recommendForProject(javaProjectId, javaOwnerId).stream()
				.noneMatch(recommendation -> recommendation.name().equals("student3")));
		Long student2Id = studentRepository.findByEmail("student2@gmail.com").orElseThrow().getId();
		assertTrue(matchingService.recommendForProject(javaProjectId, student2Id).stream()
				.noneMatch(recommendation -> recommendation.name().equals("student3")));
		assertEquals(2, studentTeamService.findTeams(student2Id, student2Id).size());

		DemoDataSeeder.SeedSummary secondRun = seeder.seedInTransaction();
		assertEquals(0, secondRun.studentsCreated());
		assertEquals(10, secondRun.studentsReused());
		assertEquals(20, secondRun.catalogSkillsReused());
		assertEquals(53, secondRun.studentSkillsReused());
		assertEquals(8, secondRun.projectsReused());
		assertEquals(8, secondRun.teamsReused());
		assertEquals(11, secondRun.membershipsReused());
		assertEquals(6, secondRun.requestsReused());
		assertEquals(10, studentRepository.count());
		assertEquals(8, projectRepository.count());
		assertEquals(8, teamRepository.count());
		assertEquals(11, teamMembershipRepository.count());
		assertEquals(6, teamRequestRepository.count());
	}

	@Test
	void refusesExistingDemoInterestsWithCaseVariantDuplicatesAndRollsBack() {
		Student existingStudent = createStudent(1, "Existing demo profile");
		existingStudent.setInterests(new LinkedHashSet<>(List.of("Robotics", "robotics")));
		studentRepository.save(existingStudent);

		assertThrows(DuplicateResourceException.class, () -> seeder.seedInTransaction());

		assertEquals(Set.of("Robotics", "robotics"),
				new HashSet<>(studentInterestService.findByStudentId(existingStudent.getId()).interests()));
		assertEquals(1, studentRepository.count());
		assertEquals(0, skillRepository.count());
		assertEquals(0, studentSkillRepository.count());
		assertEquals(0, projectRepository.count());
		assertEquals(0, teamRepository.count());
	}

	@Test
	void refusesReusedTeamWithoutOwnerMembershipAndRollsBackSeedChanges() {
		assertInvalidOwnerMembership(null, null,
				"An existing demo team has no membership for its project owner.");
	}

	@Test
	void refusesReusedTeamWithInactiveOwnerMembershipAndRollsBackSeedChanges() {
		assertInvalidOwnerMembership(TeamMemberRole.LEADER, MembershipStatus.LEFT,
				"An existing demo team's project owner membership is not ACTIVE.");
	}

	@Test
	void refusesReusedTeamWithNonLeaderOwnerMembershipAndRollsBackSeedChanges() {
		assertInvalidOwnerMembership(TeamMemberRole.MEMBER, MembershipStatus.ACTIVE,
				"An existing demo team's project owner membership is not LEADER.");
	}

	@Test
	void refusesExistingProjectWithDifferentDescriptionAndRollsBackSeedChanges() {
		Student owner = createStudent(1, "Existing demo owner");
		var javaSkill = skillService.create(new SkillRequest("Java"));
		var springSkill = skillService.create(new SkillRequest("Spring Boot"));
		projectService.create(new ProjectCreateRequest("Java Application Development",
				"Conflicting description", owner.getId(), 4, List.of(javaSkill.id(), springSkill.id())));

		long initialStudents = studentRepository.count();
		long initialSkills = skillRepository.count();
		long initialProjects = projectRepository.count();
		long initialStudentSkills = studentSkillRepository.count();
		IllegalStateException failure = assertThrows(IllegalStateException.class,
				() -> seeder.seedInTransaction());

		assertEquals("A project title conflicts with an existing project description; "
				+ "no existing project was changed.", failure.getMessage());
		assertEquals(initialStudents, studentRepository.count());
		assertEquals(initialSkills, skillRepository.count());
		assertEquals(initialProjects, projectRepository.count());
		assertEquals(initialStudentSkills, studentSkillRepository.count());
		assertEquals("Conflicting description",
				projectService.findAll().stream()
						.filter(project -> project.title().equals("Java Application Development"))
						.findFirst().orElseThrow().description());
	}

	@Test
	void refusesAcceptedRequestWithoutRecipientMembershipAndRollsBackSeedChanges() {
		assertInvalidAcceptedRequestMembership(null, null,
				"An accepted demo request has no corresponding team membership for its recipient.");
	}

	@Test
	void refusesAcceptedRequestWithInactiveRecipientMembershipAndRollsBackSeedChanges() {
		assertInvalidAcceptedRequestMembership(TeamMemberRole.MEMBER, MembershipStatus.LEFT,
				"An accepted demo request recipient's team membership is not ACTIVE.");
	}

	@Test
	void refusesAcceptedRequestWithNonMemberRecipientRoleAndRollsBackSeedChanges() {
		assertInvalidAcceptedRequestMembership(TeamMemberRole.LEADER, MembershipStatus.ACTIVE,
				"An accepted demo request recipient's team membership is not MEMBER.");
	}

	private void assertInvalidAcceptedRequestMembership(TeamMemberRole role, MembershipStatus status,
			String expectedMessage) {
		Student owner = createStudent(7, "Existing demo owner");
		Student recipient = createStudent(8, "Existing demo recipient");
		var verilogSkill = skillService.create(new SkillRequest("Verilog"));
		var vlsiSkill = skillService.create(new SkillRequest("VLSI Design"));
		var projectResponse = projectService.create(new ProjectCreateRequest("VLSI Design and Verification",
				"Design and verify a compact digital module using a reproducible simulation workflow.",
				owner.getId(), 4, List.of(verilogSkill.id(), vlsiSkill.id())));
		var project = projectRepository.findById(projectResponse.id()).orElseThrow();
		Team team = teamRepository.findByProject_Id(projectResponse.id()).orElseThrow();
		if (role != null) {
			TeamMembership recipientMembership = new TeamMembership(team, recipient, role);
			recipientMembership.setStatus(status);
			teamMembershipRepository.save(recipientMembership);
		}
		TeamRequest request = new TeamRequest(owner, recipient, team);
		request.setStatus(TeamRequestStatus.ACCEPTED);
		teamRequestRepository.save(request);

		long initialStudents = studentRepository.count();
		long initialSkills = skillRepository.count();
		long initialProjects = projectRepository.count();
		long initialTeams = teamRepository.count();
		long initialMemberships = teamMembershipRepository.count();
		long initialRequests = teamRequestRepository.count();
		long initialStudentSkills = studentSkillRepository.count();
		IllegalStateException failure = assertThrows(IllegalStateException.class,
				() -> seeder.seedInTransaction());

		assertEquals(expectedMessage, failure.getMessage());
		assertEquals(initialStudents, studentRepository.count());
		assertEquals(initialSkills, skillRepository.count());
		assertEquals(initialProjects, projectRepository.count());
		assertEquals(initialTeams, teamRepository.count());
		assertEquals(initialMemberships, teamMembershipRepository.count());
		assertEquals(initialRequests, teamRequestRepository.count());
		assertEquals(initialStudentSkills, studentSkillRepository.count());
	}

	private void assertInvalidOwnerMembership(TeamMemberRole role, MembershipStatus status,
			String expectedMessage) {
		Student owner = createStudent(1, "Existing demo owner");
		var javaSkill = skillService.create(new SkillRequest("Java"));
		var springSkill = skillService.create(new SkillRequest("Spring Boot"));
		var project = projectService.create(new ProjectCreateRequest("Java Application Development",
				"Build a maintainable Java application with a documented API and automated tests.",
				owner.getId(), 4, List.of(javaSkill.id(), springSkill.id())));
		Team team = teamRepository.findByProject_Id(project.id()).orElseThrow();
		TeamMembership membership = teamMembershipRepository
				.findByTeam_IdAndStudent_Id(team.getId(), owner.getId()).orElseThrow();
		if (role == null) {
			teamMembershipRepository.delete(membership);
		} else {
			membership.setRole(role);
			membership.setStatus(status);
			teamMembershipRepository.save(membership);
		}

		long initialStudents = studentRepository.count();
		long initialSkills = skillRepository.count();
		long initialProjects = projectRepository.count();
		long initialTeams = teamRepository.count();
		long initialMemberships = teamMembershipRepository.count();
		IllegalStateException failure = assertThrows(IllegalStateException.class,
				() -> seeder.seedInTransaction());

		assertEquals(expectedMessage, failure.getMessage());
		assertEquals(initialStudents, studentRepository.count());
		assertEquals(initialSkills, skillRepository.count());
		assertEquals(initialProjects, projectRepository.count());
		assertEquals(initialTeams, teamRepository.count());
		assertEquals(initialMemberships, teamMembershipRepository.count());
		assertEquals(0, teamRequestRepository.count());
		assertEquals(0, studentSkillRepository.count());
	}

	private Student createStudent(int number, String name) {
		StudentCreateRequest request = new StudentCreateRequest();
		request.setName(name);
		request.setRollNumber("DEMO-STUDENT-%02d".formatted(number));
		request.setEmail("student" + number + "@gmail.com");
		request.setPassword("student" + number + "@gmail.com");
		request.setDepartment("CSE");
		request.setAcademicYear(3);
		studentService.create(request);
		return studentRepository.findByEmail(request.getEmail()).orElseThrow();
	}
}

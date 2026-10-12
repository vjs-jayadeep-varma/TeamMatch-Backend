package com.teammatch.backend.config;

import com.teammatch.backend.dto.ProjectCreateRequest;
import com.teammatch.backend.dto.ProjectResponse;
import com.teammatch.backend.dto.SkillResponse;
import com.teammatch.backend.dto.StudentCreateRequest;
import com.teammatch.backend.dto.StudentSkillNameAssignmentRequest;
import com.teammatch.backend.dto.TeamRequestCreateRequest;
import com.teammatch.backend.dto.TeamRequestDecisionRequest;
import com.teammatch.backend.dto.TeamRequestResponse;
import com.teammatch.backend.entity.enums.AccountRole;
import com.teammatch.backend.entity.enums.MembershipStatus;
import com.teammatch.backend.entity.enums.ProjectStatus;
import com.teammatch.backend.entity.enums.SkillProficiency;
import com.teammatch.backend.entity.enums.TeamMemberRole;
import com.teammatch.backend.entity.enums.TeamRequestStatus;
import com.teammatch.backend.entity.enums.TeamStatus;
import com.teammatch.backend.exception.DuplicateResourceException;
import com.teammatch.backend.repository.StudentRepository;
import com.teammatch.backend.repository.TeamMembershipRepository;
import com.teammatch.backend.repository.TeamRepository;
import com.teammatch.backend.repository.TeamRequestRepository;
import com.teammatch.backend.service.ProjectService;
import com.teammatch.backend.service.SkillService;
import com.teammatch.backend.service.StudentInterestService;
import com.teammatch.backend.service.StudentService;
import com.teammatch.backend.service.StudentSkillService;
import com.teammatch.backend.service.TeamRequestService;
import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Profile("dev & !prod")
@ConditionalOnProperty(prefix = "teammatch.demo-seed", name = "enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {
	private static final Logger LOGGER = LoggerFactory.getLogger(DemoDataSeeder.class);
	private static final List<String> CATALOG_SKILLS = List.of(
			"Java", "Python", "C", "C++", "JavaScript", "React", "SQL", "Spring Boot",
			"Machine Learning", "Data Structures and Algorithms", "Digital Electronics", "Verilog",
			"VLSI Design", "FPGA", "MATLAB", "Signal Processing", "Embedded Systems", "IoT",
			"PCB Design", "Computer Networks");
	private static final List<StudentSeed> STUDENTS = List.of(
			new StudentSeed(1, "CSE", 3, "Backend systems, open-source software",
					"Java", "Spring Boot", "SQL", "Data Structures and Algorithms", "JavaScript"),
			new StudentSeed(2, "CSE", 2, "Web applications, software design",
					"Java", "Spring Boot", "React", "SQL", "Computer Networks"),
			new StudentSeed(3, "IT", 3, "Automation, machine learning",
					"Java", "JavaScript", "React", "Python", "Data Structures and Algorithms", "Machine Learning"),
			new StudentSeed(4, "CSE", 4, "Data engineering, automation",
					"Python", "SQL", "Java", "Machine Learning", "Data Structures and Algorithms"),
			new StudentSeed(5, "IT", 2, "Applied machine learning, web development",
					"Python", "SQL", "Machine Learning", "JavaScript", "React"),
			new StudentSeed(6, "CSE", 3, "Systems programming, network tools",
					"Python", "C", "C++", "SQL", "Computer Networks"),
			new StudentSeed(7, "ECE", 4, "Digital design, chip verification",
					"C", "C++", "Verilog", "VLSI Design", "Digital Electronics", "MATLAB"),
			new StudentSeed(8, "EEE", 3, "FPGA prototyping, signal analysis",
					"Verilog", "VLSI Design", "FPGA", "Digital Electronics", "Signal Processing", "MATLAB"),
			new StudentSeed(9, "ECE", 3, "Connected devices, embedded prototyping",
					"IoT", "Embedded Systems", "C", "PCB Design", "Computer Networks"),
			new StudentSeed(10, "EEE", 2, "Embedded control, sensor systems",
					"IoT", "Embedded Systems", "C++", "Signal Processing", "MATLAB"));
	private static final List<ProjectSeed> PROJECTS = List.of(
			new ProjectSeed("Java Application Development",
					"Build a maintainable Java application with a documented API and automated tests.",
					1, 4, "Java", "Spring Boot"),
			new ProjectSeed("Python Automation Toolkit",
					"Create reusable Python tools that automate common data and workflow tasks.",
					4, 4, "Python", "SQL"),
			new ProjectSeed("VLSI Design and Verification",
					"Design and verify a compact digital module using a reproducible simulation workflow.",
					7, 4, "Verilog", "VLSI Design"),
			new ProjectSeed("Machine Learning Prediction System",
					"Train and evaluate a transparent prediction model on a documented sample dataset.",
					3, 4, "Python", "Machine Learning"),
			new ProjectSeed("React Student Portal",
					"Develop an accessible student portal with responsive React views and API integration.",
					2, 4, "JavaScript", "React"),
			new ProjectSeed("IoT Smart Monitoring System",
					"Prototype a connected sensor monitor with clear device and data-flow documentation.",
					9, 4, "IoT", "Embedded Systems"),
			new ProjectSeed("Signal Processing Analysis Tool",
					"Build a MATLAB-based tool for exploring and visualizing sampled signal data.",
					8, 4, "Signal Processing", "MATLAB"),
			new ProjectSeed("Embedded Systems Controller",
					"Implement a small embedded controller with documented interfaces and test scenarios.",
					10, 4, "Embedded Systems", "C++"));
	private static final List<RequestSeed> REQUESTS = List.of(
			new RequestSeed(1, "Java Application Development", 2, TeamRequestStatus.ACCEPTED),
			new RequestSeed(1, "Java Application Development", 3, TeamRequestStatus.PENDING),
			new RequestSeed(4, "Python Automation Toolkit", 5, TeamRequestStatus.ACCEPTED),
			new RequestSeed(4, "Python Automation Toolkit", 6, TeamRequestStatus.REJECTED),
			new RequestSeed(7, "VLSI Design and Verification", 8, TeamRequestStatus.ACCEPTED),
			new RequestSeed(9, "IoT Smart Monitoring System", 10, TeamRequestStatus.PENDING));

	private final StudentService studentService;
	private final StudentRepository studentRepository;
	private final SkillService skillService;
	private final StudentSkillService studentSkillService;
	private final StudentInterestService studentInterestService;
	private final ProjectService projectService;
	private final TeamRequestService teamRequestService;
	private final TeamRepository teamRepository;
	private final TeamMembershipRepository teamMembershipRepository;
	private final TeamRequestRepository teamRequestRepository;
	private final TransactionTemplate transactionTemplate;
	private final Environment environment;

	public DemoDataSeeder(StudentService studentService, StudentRepository studentRepository,
			SkillService skillService, StudentSkillService studentSkillService,
			StudentInterestService studentInterestService,
			ProjectService projectService, TeamRequestService teamRequestService, TeamRepository teamRepository,
			TeamMembershipRepository teamMembershipRepository, TeamRequestRepository teamRequestRepository,
			PlatformTransactionManager transactionManager, Environment environment) {
		this.studentService = studentService;
		this.studentRepository = studentRepository;
		this.skillService = skillService;
		this.studentSkillService = studentSkillService;
		this.studentInterestService = studentInterestService;
		this.projectService = projectService;
		this.teamRequestService = teamRequestService;
		this.teamRepository = teamRepository;
		this.teamMembershipRepository = teamMembershipRepository;
		this.teamRequestRepository = teamRequestRepository;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
		this.environment = environment;
	}

	@Override
	public void run(ApplicationArguments args) {
		validateLocalDatabaseTarget();
		SeedSummary summary = seedInTransaction();
		if (summary == null) {
			throw new IllegalStateException("Demo data seeding did not produce a result.");
		}
		LOGGER.info("Demo data seed complete: students {}/{}, catalog skills {}/{}, student skills {}/{}, "
						+ "projects {}/{}, teams {}/{}, memberships {}/{}, requests {}/{} (created/reused).",
				summary.studentsCreated(), summary.studentsReused(),
				summary.catalogSkillsCreated(), summary.catalogSkillsReused(),
				summary.studentSkillsAdded(), summary.studentSkillsReused(),
				summary.projectsCreated(), summary.projectsReused(),
				summary.teamsCreated(), summary.teamsReused(),
				summary.membershipsCreated(), summary.membershipsReused(),
				summary.requestsCreated(), summary.requestsReused());
	}

	SeedSummary seedInTransaction() {
		return transactionTemplate.execute(status -> seed());
	}

	private void validateLocalDatabaseTarget() {
		if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
			throw new IllegalStateException("Demo data seeding is disabled for production.");
		}
		String jdbcUrl = environment.getProperty("spring.datasource.url", "");
		try {
			URI databaseUri = URI.create(jdbcUrl.substring("jdbc:mysql:".length()));
			String databaseName = databaseUri.getPath().replaceFirst("^/", "").split("/")[0];
			String host = databaseUri.getHost();
			if (!jdbcUrl.startsWith("jdbc:mysql://") || !databaseName.equals("teammatch_db")
					|| !("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)
							|| "::1".equals(host))) {
				throw new IllegalStateException(
						"Demo data seeding requires a local MySQL database named teammatch_db.");
			}
		} catch (RuntimeException exception) {
			if (exception instanceof IllegalStateException) {
				throw exception;
			}
			throw new IllegalStateException(
					"Demo data seeding requires a local MySQL database named teammatch_db.");
		}
	}

	private SeedSummary seed() {
		validateDemoAccountConflicts();
		SeedCounters counters = new SeedCounters();
		Map<Integer, Long> studentIds = ensureStudents(counters);
		Map<String, SkillResponse> skills = ensureStudentSkills(studentIds, counters);
		Map<String, ProjectResponse> projects = ensureProjects(studentIds, skills, counters);
		ensureRequests(studentIds, projects, counters);
		return counters.toSummary();
	}

	private void validateDemoAccountConflicts() {
		for (StudentSeed seed : STUDENTS) {
			var existingStudent = studentRepository.findByEmail(emailFor(seed.number()));
			if (existingStudent.isPresent()) {
				if (existingStudent.get().getAccountRole() != AccountRole.STUDENT) {
					throw new IllegalStateException("A demo email is already used by a non-student account.");
				}
				continue;
			}
			if (studentRepository.existsByRollNumber("DEMO-STUDENT-%02d".formatted(seed.number()))) {
				throw new DuplicateResourceException("A demo roll number is already assigned to another account.");
			}
		}
	}

	private Map<Integer, Long> ensureStudents(SeedCounters counters) {
		Map<Integer, Long> studentIds = new LinkedHashMap<>();
		for (StudentSeed seed : STUDENTS) {
			String email = emailFor(seed.number());
			var existing = studentRepository.findByEmail(email);
			if (existing.isPresent()) {
				if (existing.get().getAccountRole() != AccountRole.STUDENT) {
					throw new IllegalStateException("A demo email is already used by a non-student account.");
				}
				studentIds.put(seed.number(), existing.get().getId());
				counters.studentsReused++;
				continue;
			}

			String rollNumber = "DEMO-STUDENT-%02d".formatted(seed.number());
			if (studentRepository.existsByRollNumber(rollNumber)) {
				throw new DuplicateResourceException("A demo roll number is already assigned to another account.");
			}
			StudentCreateRequest request = new StudentCreateRequest();
			request.setName("student" + seed.number());
			request.setRollNumber(rollNumber);
			request.setEmail(email);
			request.setPassword(email);
			request.setDepartment(seed.department());
			request.setAcademicYear(seed.academicYear());
			studentIds.put(seed.number(), studentService.create(request).id());
			counters.studentsCreated++;
		}
		return studentIds;
	}

	private Map<String, SkillResponse> ensureStudentSkills(Map<Integer, Long> studentIds, SeedCounters counters) {
		Map<String, SkillResponse> initialSkills = indexSkills(skillService.findAll());
		Set<String> requestedSkillNames = new LinkedHashSet<>(CATALOG_SKILLS);
		for (StudentSeed student : STUDENTS) {
			requestedSkillNames.addAll(student.skills());
		}
		for (String skillName : requestedSkillNames) {
			if (initialSkills.containsKey(normalize(skillName))) {
				counters.catalogSkillsReused++;
			} else {
				counters.catalogSkillsCreated++;
			}
		}

		for (StudentSeed seed : STUDENTS) {
			Long studentId = studentIds.get(seed.number());
			Set<Long> existingSkillIds = new LinkedHashSet<>();
			studentSkillService.findByStudentId(studentId).forEach(skill -> existingSkillIds.add(skill.skillId()));
			int skillIndex = 0;
			for (String skillName : seed.skills()) {
				SkillProficiency proficiency = SkillProficiency.values()[
						(seed.number() + skillIndex++) % SkillProficiency.values().length];
				SkillResponse existingCatalogSkill = initialSkills.get(normalize(skillName));
				if (existingCatalogSkill != null && existingSkillIds.contains(existingCatalogSkill.id())) {
					counters.studentSkillsReused++;
				} else {
					var assigned = studentSkillService.assignByName(studentId,
							new StudentSkillNameAssignmentRequest(skillName, proficiency));
					existingSkillIds.add(assigned.skillId());
					counters.studentSkillsAdded++;
				}
			}
			mergeInterests(studentId, seed.interests());
		}

		Map<String, SkillResponse> allSkills = indexSkills(skillService.findAll());
		Map<String, SkillResponse> result = new LinkedHashMap<>();
		for (String skillName : requestedSkillNames) {
			SkillResponse skill = allSkills.get(normalize(skillName));
			if (skill == null) {
				throw new IllegalStateException("A required demo skill was not persisted.");
			}
			result.put(normalize(skillName), skill);
		}
		return result;
	}

	private void mergeInterests(Long studentId, List<String> additions) {
		studentInterestService.addMissing(studentId, additions);
	}

	private Map<String, ProjectResponse> ensureProjects(Map<Integer, Long> studentIds,
			Map<String, SkillResponse> skills, SeedCounters counters) {
		List<ProjectResponse> existingProjects = projectService.findAll();
		Map<String, ProjectResponse> byTitle = new LinkedHashMap<>();
		for (ProjectResponse project : existingProjects) {
			String key = normalize(project.title());
			if (byTitle.putIfAbsent(key, project) != null) {
				throw new IllegalStateException("Multiple projects use a demo project title; refusing to guess.");
			}
		}

		Map<String, ProjectResponse> result = new LinkedHashMap<>();
		for (ProjectSeed seed : PROJECTS) {
			String titleKey = normalize(seed.title());
			Long ownerId = studentIds.get(seed.ownerNumber());
			List<Long> skillIds = seed.requiredSkills().stream()
					.map(name -> skills.get(normalize(name)).id())
					.toList();
			ProjectResponse project = byTitle.get(titleKey);
			if (project == null) {
				project = projectService.create(new ProjectCreateRequest(seed.title(), seed.description(), ownerId,
						seed.teamSize(), skillIds));
				counters.projectsCreated++;
				counters.teamsCreated++;
				counters.membershipsCreated++;
			} else {
				Set<String> existingRequiredSkills = new LinkedHashSet<>();
				project.requiredSkills().forEach(skill -> existingRequiredSkills.add(normalize(skill.name())));
				Set<String> plannedRequiredSkills = new LinkedHashSet<>();
				seed.requiredSkills().forEach(name -> plannedRequiredSkills.add(normalize(name)));
				if (!seed.description().equals(project.description())) {
					throw new IllegalStateException(
							"A project title conflicts with an existing project description; no existing project was changed.");
				}
				if (!ownerId.equals(project.ownerStudentId()) || project.requiredTeamSize() != seed.teamSize()
						|| !plannedRequiredSkills.equals(existingRequiredSkills)
						|| (project.status() != ProjectStatus.OPEN && project.status() != ProjectStatus.IN_PROGRESS)) {
					throw new IllegalStateException(
							"A project title conflicts with an existing project; no existing project was changed.");
				}
				counters.projectsReused++;
			}
			result.put(titleKey, project);
		}
		return result;
	}

	private void ensureRequests(Map<Integer, Long> studentIds, Map<String, ProjectResponse> projects,
			SeedCounters counters) {
		Map<String, Long> startingMembershipCounts = new LinkedHashMap<>();
		Map<String, Boolean> startingTeams = new LinkedHashMap<>();
		for (ProjectSeed projectSeed : PROJECTS) {
			String key = normalize(projectSeed.title());
			ProjectResponse project = projects.get(key);
			var team = teamRepository.findByProject_Id(project.id());
			startingTeams.put(key, team.isPresent());
			startingMembershipCounts.put(key, team.map(existingTeam ->
					teamMembershipRepository.countByTeam_IdAndStatus(existingTeam.getId(), MembershipStatus.ACTIVE))
					.orElse(0L));
		}

		for (RequestSeed seed : REQUESTS) {
			ProjectResponse project = projects.get(normalize(seed.projectTitle()));
			Long requesterId = studentIds.get(seed.requesterNumber());
			Long recipientId = studentIds.get(seed.recipientNumber());
			var team = teamRepository.findByProject_Id(project.id());
			if (team.isPresent() && team.get().getStatus() == TeamStatus.DISBANDED) {
				throw new IllegalStateException("A demo project already has a disbanded team; refusing to change it.");
			}
			if (team.isPresent()) {
				var ownerMembership = teamMembershipRepository.findByTeam_IdAndStudent_Id(
						team.get().getId(), requesterId);
				if (ownerMembership.isEmpty()) {
					throw new IllegalStateException(
							"An existing demo team has no membership for its project owner.");
				}
				if (ownerMembership.get().getStatus() != MembershipStatus.ACTIVE) {
					throw new IllegalStateException(
							"An existing demo team's project owner membership is not ACTIVE.");
				}
				if (ownerMembership.get().getRole() != TeamMemberRole.LEADER) {
					throw new IllegalStateException(
							"An existing demo team's project owner membership is not LEADER.");
				}
			}
			List<TeamRequestResponse> matchingRequests = team
					.map(existingTeam -> teamRequestRepository.findAllByRequestedTeam_IdAndRequestingStudent_IdAndRecipientStudent_IdOrderByCreatedAtDesc(
									existingTeam.getId(), requesterId, recipientId)
							.stream()
							.map(request -> new TeamRequestResponse(request.getId(), project.id(), project.title(),
									requesterId, request.getRequestingStudent().getName(), recipientId,
									request.getRecipientStudent().getName(), request.getStatus(),
									request.getCreatedAt(), request.getUpdatedAt()))
							.toList())
					.orElseGet(List::of);
			if (matchingRequests.size() > 1) {
				throw new IllegalStateException("Duplicate demo team requests already exist; refusing to change them.");
			}

			TeamRequestResponse request;
			boolean createdInThisRun = matchingRequests.isEmpty();
			if (createdInThisRun) {
				if (team.isPresent() && teamMembershipRepository.findByTeam_IdAndStudent_Id(
						team.get().getId(), recipientId).isPresent()) {
					throw new IllegalStateException(
							"A planned request recipient already has a membership; refusing to alter it.");
				}
				request = teamRequestService.create(requesterId, project.id(),
						new TeamRequestCreateRequest(recipientId));
				counters.requestsCreated++;
			} else {
				request = matchingRequests.get(0);
				counters.requestsReused++;
				if (request.status() != seed.status()) {
					throw new IllegalStateException(
							"An existing demo request has a different status; refusing to change it.");
				}
			}

			if (createdInThisRun && request.status() == TeamRequestStatus.PENDING
					&& seed.status() != TeamRequestStatus.PENDING) {
				request = teamRequestService.respond(recipientId, recipientId, request.id(),
						new TeamRequestDecisionRequest(seed.status()));
			}
			if (request.status() == TeamRequestStatus.ACCEPTED) {
				var acceptedTeam = teamRepository.findByProject_Id(project.id()).orElseThrow();
				var membership = teamMembershipRepository.findByTeam_IdAndStudent_Id(
						acceptedTeam.getId(), recipientId);
				if (membership.isEmpty()) {
					throw new IllegalStateException(
							"An accepted demo request has no corresponding team membership for its recipient.");
				}
				if (membership.get().getStatus() != MembershipStatus.ACTIVE) {
					throw new IllegalStateException(
							"An accepted demo request recipient's team membership is not ACTIVE.");
				}
				if (membership.get().getRole() != TeamMemberRole.MEMBER) {
					throw new IllegalStateException(
							"An accepted demo request recipient's team membership is not MEMBER.");
				}
			}
		}

		for (ProjectSeed projectSeed : PROJECTS) {
			String key = normalize(projectSeed.title());
			ProjectResponse project = projects.get(key);
			var team = teamRepository.findByProject_Id(project.id());
			if (team.isEmpty()) {
				continue;
			}
			long currentMembershipCount = teamMembershipRepository.countByTeam_IdAndStatus(
					team.get().getId(), MembershipStatus.ACTIVE);
			if (currentMembershipCount + teamRequestRepository.countByRequestedTeam_IdAndStatus(
					team.get().getId(), TeamRequestStatus.PENDING) > project.requiredTeamSize()) {
				throw new IllegalStateException("A demo team's active members and pending requests exceed capacity.");
			}
			counters.teamsCreated += startingTeams.get(key) ? 0 : 1;
			counters.teamsReused += startingTeams.get(key) ? 1 : 0;
			counters.membershipsCreated += Math.max(0,
					currentMembershipCount - startingMembershipCounts.get(key));
			counters.membershipsReused += startingMembershipCounts.get(key);
		}
	}

	private Map<String, SkillResponse> indexSkills(List<SkillResponse> skills) {
		Map<String, SkillResponse> result = new LinkedHashMap<>();
		for (SkillResponse skill : skills) {
			result.putIfAbsent(normalize(skill.name()), skill);
		}
		return result;
	}

	private String emailFor(int number) {
		return "student" + number + "@gmail.com";
	}

	private String normalize(String value) {
		return value.trim().toLowerCase(Locale.ROOT);
	}

	private record StudentSeed(int number, String department, int academicYear, List<String> interests,
			List<String> skills) {
		private StudentSeed(int number, String department, int academicYear, String interests, String... skills) {
			this(number, department, academicYear, Arrays.asList(interests.split(", ")), List.of(skills));
		}
	}

	private record ProjectSeed(String title, String description, int ownerNumber, int teamSize,
			List<String> requiredSkills) {
		private ProjectSeed(String title, String description, int ownerNumber, int teamSize, String... skills) {
			this(title, description, ownerNumber, teamSize, List.of(skills));
		}
	}

	private record RequestSeed(int requesterNumber, String projectTitle, int recipientNumber,
			TeamRequestStatus status) {
	}

	record SeedSummary(int studentsCreated, int studentsReused, int catalogSkillsCreated,
			int catalogSkillsReused, int studentSkillsAdded, int studentSkillsReused, int projectsCreated,
			int projectsReused, int teamsCreated, int teamsReused, int membershipsCreated, int membershipsReused,
			int requestsCreated, int requestsReused) {
	}

	private static final class SeedCounters {
		private int studentsCreated;
		private int studentsReused;
		private int catalogSkillsCreated;
		private int catalogSkillsReused;
		private int studentSkillsAdded;
		private int studentSkillsReused;
		private int projectsCreated;
		private int projectsReused;
		private int teamsCreated;
		private int teamsReused;
		private int membershipsCreated;
		private int membershipsReused;
		private int requestsCreated;
		private int requestsReused;

		private SeedSummary toSummary() {
			return new SeedSummary(studentsCreated, studentsReused, catalogSkillsCreated, catalogSkillsReused,
					studentSkillsAdded, studentSkillsReused, projectsCreated, projectsReused, teamsCreated,
					teamsReused, membershipsCreated, membershipsReused, requestsCreated, requestsReused);
		}
	}
}

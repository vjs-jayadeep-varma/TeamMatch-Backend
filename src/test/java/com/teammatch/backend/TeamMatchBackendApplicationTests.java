package com.teammatch.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.teammatch.backend.entity.Project;
import com.teammatch.backend.entity.Skill;
import com.teammatch.backend.entity.Student;
import com.teammatch.backend.entity.StudentSkill;
import com.teammatch.backend.entity.Team;
import com.teammatch.backend.entity.TeamMembership;
import com.teammatch.backend.entity.enums.ProjectStatus;
import com.teammatch.backend.entity.enums.SkillProficiency;
import com.teammatch.backend.entity.enums.TeamMemberRole;
import com.teammatch.backend.repository.ProjectRepository;
import com.teammatch.backend.repository.StudentRepository;
import com.teammatch.backend.service.ProjectService;
import jakarta.persistence.EntityManager;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:teammatch-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class TeamMatchBackendApplicationTests {
	@Autowired
	private EntityManager entityManager;

	@Autowired
	private StudentRepository studentRepository;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectService projectService;

	@Test
	void contextLoads() {
	}

	@Test
	@Transactional
	void administratorSearchQueriesCoverProfilesSkillsInterestsProjectsAndMembers() {
		Student owner = new Student("Alex Owner", "R-SEARCH-1", "alex-search@example.com",
				"not-a-real-login-hash", "CSE", 2);
		owner.setInterests(Set.of("Robotics"));
		Student member = new Student("Taylor SearchMember", "R-SEARCH-2", "taylor-search@example.com",
				"not-a-real-login-hash", "ECE", 3);
		Skill skill = new Skill("Kotlin");
		entityManager.persist(owner);
		entityManager.persist(member);
		entityManager.persist(skill);
		Project project = new Project("Mobile Search Project", "Project search text", owner,
				Set.of(skill), 4, ProjectStatus.OPEN);
		entityManager.persist(project);
		entityManager.persist(new StudentSkill(owner, skill, SkillProficiency.ADVANCED));
		Team team = new Team(project, "Mobile Search Team");
		entityManager.persist(team);
		entityManager.persist(new TeamMembership(team, owner, TeamMemberRole.LEADER));
		entityManager.persist(new TeamMembership(team, member, TeamMemberRole.MEMBER));
		entityManager.flush();
		entityManager.clear();

		assertEquals("alex-search@example.com", studentRepository.searchAdminStudents(
				"robotics", PageRequest.of(0, 20)).getContent().get(0).getEmail());
		assertEquals("alex-search@example.com", studentRepository.searchAdminStudents(
				"kotlin", PageRequest.of(0, 20)).getContent().get(0).getEmail());
		assertTrue(studentRepository.searchAdminStudents(
				"mobile search project", PageRequest.of(0, 20)).getContent().stream()
						.anyMatch(student -> student.getEmail().equals("alex-search@example.com")));
		assertEquals("taylor-search@example.com", studentRepository.searchAdminStudents(
				"mobile search project", PageRequest.of(0, 20)).getContent().stream()
						.filter(student -> student.getEmail().equals("taylor-search@example.com"))
						.findFirst().orElseThrow().getEmail());
		assertEquals("Mobile Search Project", projectRepository.searchAdminProjects(
				"kotlin", PageRequest.of(0, 20)).getContent().get(0).getTitle());
		assertEquals("Mobile Search Project", projectRepository.searchAdminProjects(
				"Taylor SearchMember", PageRequest.of(0, 20)).getContent().get(0).getTitle());
	}

	@Test
	@Transactional
	void closingProjectPersistsCancelledStatusInIsolatedDatabase() {
		Student owner = new Student("Project Owner", "R-CLOSE-1", "project-owner-close@example.com",
				"not-a-real-login-hash", "CSE", 2);
		entityManager.persist(owner);
		Project project = new Project("Closeable Project", "Project closure test", owner,
				Set.of(), 3, ProjectStatus.OPEN);
		entityManager.persist(project);
		entityManager.flush();
		Long projectId = project.getId();

		projectService.close(projectId, owner.getId());

		entityManager.flush();
		entityManager.clear();
		assertEquals(ProjectStatus.CANCELLED, projectRepository.findById(projectId).orElseThrow().getStatus());
	}

}

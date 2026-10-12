package com.teammatch.backend.repository;

import com.teammatch.backend.entity.Project;
import com.teammatch.backend.dto.AdminProjectSummary;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
	@Query("select new com.teammatch.backend.dto.AdminProjectSummary(project.id, project.title, project.status, "
			+ "project.requiredTeamSize, project.owner.name) from Project project order by project.id")
	List<AdminProjectSummary> findAllAdminSummaries();

	@Query("select project from Project project where "
			+ "(:query = '' or lower(project.title) like lower(concat('%', :query, '%')) "
			+ "or lower(project.description) like lower(concat('%', :query, '%')) "
			+ "or lower(project.owner.name) like lower(concat('%', :query, '%')) "
			+ "or exists (select skill.id from Project requiredProject "
			+ "join requiredProject.requiredSkills skill where requiredProject = project "
			+ "and lower(skill.name) like lower(concat('%', :query, '%'))) "
			+ "or exists (select membership.id from TeamMembership membership "
			+ "join membership.team team join team.project memberProject "
			+ "join membership.student member where memberProject = project "
			+ "and lower(member.name) like lower(concat('%', :query, '%'))))")
	Page<Project> searchAdminProjects(@Param("query") String query, Pageable pageable);

	List<Project> findAllByOwner_Id(Long ownerId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select project from Project project where project.id = :id")
	Optional<Project> findByIdForUpdate(@Param("id") Long id);
}
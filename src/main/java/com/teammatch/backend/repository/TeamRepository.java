package com.teammatch.backend.repository;

import com.teammatch.backend.entity.Team;
import com.teammatch.backend.dto.AdminTeamMemberRow;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {
	@Query("select new com.teammatch.backend.dto.AdminTeamMemberRow(team.id, team.teamName, team.status, "
			+ "project.id, project.title, student.id, student.name, membership.role, membership.status) "
			+ "from Team team join team.project project left join TeamMembership membership on membership.team = team "
			+ "left join membership.student student order by team.id, student.id")
	java.util.List<AdminTeamMemberRow> findAllAdminSummaries();

	Optional<Team> findByProject_Id(Long projectId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select team from Team team where team.project.id = :projectId")
	Optional<Team> findByProject_IdForUpdate(@Param("projectId") Long projectId);
}
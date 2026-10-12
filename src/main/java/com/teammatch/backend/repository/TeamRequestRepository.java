package com.teammatch.backend.repository;

import com.teammatch.backend.entity.TeamRequest;
import com.teammatch.backend.dto.AdminTeamRequestSummary;
import com.teammatch.backend.entity.enums.TeamRequestStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRequestRepository extends JpaRepository<TeamRequest, Long> {
	@Query("select new com.teammatch.backend.dto.AdminTeamRequestSummary(request.id, project.title, "
			+ "request.requestingStudent.name, request.recipientStudent.name, request.status, "
			+ "request.createdAt, request.updatedAt) from TeamRequest request "
			+ "join request.requestedTeam team join team.project project order by request.createdAt desc")
	List<AdminTeamRequestSummary> findAllAdminSummaries();

	@Query("select request from TeamRequest request join request.requestedTeam team "
			+ "join team.project project where (:status is null or request.status = :status) "
			+ "and (:projectId is null or project.id = :projectId) order by request.createdAt desc")
	Page<TeamRequest> findAdminRequests(@Param("status") TeamRequestStatus status,
			@Param("projectId") Long projectId, Pageable pageable);

	List<TeamRequest> findAllByRequestingStudent_IdOrderByCreatedAtDesc(Long studentId);

	List<TeamRequest> findAllByRecipientStudent_IdOrderByCreatedAtDesc(Long studentId);

	List<TeamRequest> findAllByRequestingStudent_IdAndStatusOrderByCreatedAtDesc(
			Long studentId, TeamRequestStatus status);

	List<TeamRequest> findAllByRecipientStudent_IdAndStatusOrderByCreatedAtDesc(
			Long studentId, TeamRequestStatus status);

	List<TeamRequest> findAllByRequestingStudent_IdAndStatusInOrderByCreatedAtDesc(
			Long studentId, List<TeamRequestStatus> statuses);

	List<TeamRequest> findAllByRecipientStudent_IdAndStatusInOrderByCreatedAtDesc(
			Long studentId, List<TeamRequestStatus> statuses);

	List<TeamRequest> findAllByRequestedTeam_IdOrderByCreatedAtDesc(Long teamId);

	List<TeamRequest> findAllByRequestedTeam_IdAndRequestingStudent_IdAndRecipientStudent_IdOrderByCreatedAtDesc(
			Long teamId, Long requestingStudentId, Long recipientStudentId);

	boolean existsByRequestedTeam_IdAndRecipientStudent_IdAndStatusIn(
			Long teamId, Long recipientStudentId, List<TeamRequestStatus> statuses);

	boolean existsByRequestedTeam_IdAndRequestingStudent_IdAndRecipientStudent_IdAndStatusIn(
			Long teamId, Long requestingStudentId, Long recipientStudentId, List<TeamRequestStatus> statuses);

	long countByRequestedTeam_IdAndStatus(Long teamId, TeamRequestStatus status);

	long countByStatus(TeamRequestStatus status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select request from TeamRequest request where request.id = :id")
	Optional<TeamRequest> findByIdForUpdate(@Param("id") Long id);
}
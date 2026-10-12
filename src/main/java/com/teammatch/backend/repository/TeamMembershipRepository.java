package com.teammatch.backend.repository;

import com.teammatch.backend.entity.TeamMembership;
import com.teammatch.backend.entity.enums.MembershipStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMembershipRepository extends JpaRepository<TeamMembership, Long> {
	List<TeamMembership> findAllByTeam_Id(Long teamId);

	List<TeamMembership> findAllByStudent_Id(Long studentId);

	List<TeamMembership> findAllByTeam_IdAndStatus(Long teamId, MembershipStatus status);

	List<TeamMembership> findAllByStudent_IdAndStatusOrderByTeam_IdAsc(Long studentId, MembershipStatus status);

	Page<TeamMembership> findAllByStudent_Id(Long studentId, Pageable pageable);

	boolean existsByTeam_IdAndStudent_Id(Long teamId, Long studentId);

	Optional<TeamMembership> findByTeam_IdAndStudent_Id(Long teamId, Long studentId);

	long countByTeam_IdAndStatus(Long teamId, MembershipStatus status);
}
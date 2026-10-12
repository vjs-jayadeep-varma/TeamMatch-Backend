package com.teammatch.backend.dto;

import com.teammatch.backend.entity.enums.MembershipStatus;
import com.teammatch.backend.entity.enums.TeamMemberRole;
import com.teammatch.backend.entity.enums.TeamStatus;

public record AdminTeamMemberRow(
		Long teamId,
		String teamName,
		TeamStatus teamStatus,
		Long projectId,
		String projectTitle,
		Long studentId,
		String studentName,
		TeamMemberRole memberRole,
		MembershipStatus membershipStatus) {
}

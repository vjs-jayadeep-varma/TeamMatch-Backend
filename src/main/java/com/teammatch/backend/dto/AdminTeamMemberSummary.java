package com.teammatch.backend.dto;

import com.teammatch.backend.entity.enums.MembershipStatus;
import com.teammatch.backend.entity.enums.TeamMemberRole;

public record AdminTeamMemberSummary(
		Long studentId, String studentName, TeamMemberRole role, MembershipStatus status) {
}

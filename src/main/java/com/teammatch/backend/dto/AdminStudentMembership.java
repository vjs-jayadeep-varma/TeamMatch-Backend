package com.teammatch.backend.dto;

import com.teammatch.backend.entity.enums.MembershipStatus;
import com.teammatch.backend.entity.enums.TeamMemberRole;

public record AdminStudentMembership(
		Long teamId, String teamName, Long projectId, String projectTitle,
		TeamMemberRole role, MembershipStatus status) {
}

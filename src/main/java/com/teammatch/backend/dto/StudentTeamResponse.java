package com.teammatch.backend.dto;

import com.teammatch.backend.entity.enums.MembershipStatus;
import com.teammatch.backend.entity.enums.TeamMemberRole;
import com.teammatch.backend.entity.enums.TeamStatus;
import java.util.List;

public record StudentTeamResponse(
		Long teamId,
		String teamName,
		TeamStatus status,
		TeamMemberRole studentRole,
		MembershipStatus membershipStatus,
		TeamProjectResponse project,
		List<TeamMemberResponse> members) {
}

package com.teammatch.backend.dto;

import com.teammatch.backend.entity.enums.MembershipStatus;
import com.teammatch.backend.entity.enums.TeamMemberRole;

public record TeamMemberResponse(
		Long studentId,
		String name,
		String email,
		String department,
		Integer academicYear,
		TeamMemberRole role,
		MembershipStatus membershipStatus) {
}

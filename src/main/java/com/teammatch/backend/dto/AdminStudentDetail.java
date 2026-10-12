package com.teammatch.backend.dto;

import java.util.List;

public record AdminStudentDetail(
		Long id,
		String name,
		String rollNumber,
		String email,
		String department,
		Integer academicYear,
		boolean available,
		List<String> skills,
		List<String> interests,
		List<AdminProjectSummary> projects,
		List<AdminStudentMembership> memberships) {
}

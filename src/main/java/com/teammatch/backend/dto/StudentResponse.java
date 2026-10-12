package com.teammatch.backend.dto;

import com.teammatch.backend.entity.enums.AccountRole;

public record StudentResponse(
		Long id,
		String name,
		String rollNumber,
		String email,
		String department,
		Integer academicYear,
		boolean available,
		AccountRole accountRole) {
}
package com.teammatch.backend.dto;

import com.teammatch.backend.entity.enums.AccountRole;

public record AuthenticatedUserResponse(
		Long id,
		String name,
		String rollNumber,
		String email,
		String department,
		Integer academicYear,
		boolean available,
		AccountRole accountRole) {
	public static AuthenticatedUserResponse fromStudent(StudentResponse student) {
		return new AuthenticatedUserResponse(student.id(), student.name(), student.rollNumber(), student.email(),
				student.department(), student.academicYear(), student.available(), student.accountRole());
	}

	public static AuthenticatedUserResponse admin(String username) {
		return new AuthenticatedUserResponse(null, username, null, username, null, null, false, AccountRole.ADMIN);
	}
}

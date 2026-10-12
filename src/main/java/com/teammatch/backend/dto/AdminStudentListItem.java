package com.teammatch.backend.dto;

public record AdminStudentListItem(
		Long id, String name, String rollNumber, String email, String department, Integer academicYear) {
}

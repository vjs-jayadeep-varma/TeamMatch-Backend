package com.teammatch.backend.dto;

import com.teammatch.backend.entity.enums.AccountRole;

public record AdminStudentSummary(Long id, String name, String department, Integer academicYear, AccountRole accountRole) {
}

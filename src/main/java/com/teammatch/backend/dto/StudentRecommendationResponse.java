package com.teammatch.backend.dto;

import java.util.List;

public record StudentRecommendationResponse(
		Long studentId,
		String name,
		String email,
		String department,
		Integer academicYear,
		double compatibilityScore,
		List<String> matchedSkills,
		List<String> missingSkills) {
}
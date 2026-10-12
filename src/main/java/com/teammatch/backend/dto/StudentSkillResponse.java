package com.teammatch.backend.dto;

import com.teammatch.backend.entity.enums.SkillProficiency;

public record StudentSkillResponse(
		Long studentId,
		Long skillId,
		String skillName,
		SkillProficiency proficiencyLevel) {
}
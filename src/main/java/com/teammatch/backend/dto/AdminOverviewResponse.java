package com.teammatch.backend.dto;

import java.util.Map;

public record AdminOverviewResponse(
		long students, long projects, long teams, long requests, Map<String, Long> requestsByStatus) {
}

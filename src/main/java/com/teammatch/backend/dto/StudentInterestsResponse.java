package com.teammatch.backend.dto;

import java.util.List;

public record StudentInterestsResponse(Long studentId, List<String> interests) {
}

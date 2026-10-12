package com.teammatch.backend.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record AdminPage<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
	public static <T> AdminPage<T> from(Page<T> page) {
		return new AdminPage<>(page.getContent(), page.getNumber(), page.getSize(),
				page.getTotalElements(), page.getTotalPages());
	}
}

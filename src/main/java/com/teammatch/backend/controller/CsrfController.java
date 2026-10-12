package com.teammatch.backend.controller;

import java.util.Map;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/csrf")
public class CsrfController {
	@GetMapping
	public Map<String, String> token(@RequestAttribute("_csrf") CsrfToken csrfToken) {
		return Map.of(
				"token", csrfToken.getToken(),
				"headerName", csrfToken.getHeaderName());
	}
}

package com.teammatch.backend.config;

import com.teammatch.backend.entity.enums.AccountRole;
import com.teammatch.backend.security.TeamMatchPrincipal;
import java.util.Locale;
import java.util.Optional;

public class ConfiguredAdminAccount {
	private final String username;
	private final String passwordHash;

	public ConfiguredAdminAccount(String username, String passwordHash) {
		this.username = username;
		this.passwordHash = passwordHash;
	}

	public Optional<TeamMatchPrincipal> findPrincipal(String requestedUsername) {
		if (!matchesUsername(requestedUsername)) {
			return Optional.empty();
		}
		return Optional.of(new TeamMatchPrincipal(username, passwordHash, AccountRole.ADMIN));
	}

	public boolean matchesUsername(String requestedUsername) {
		return username != null && requestedUsername != null
				&& username.equals(requestedUsername.trim().toLowerCase(Locale.ROOT));
	}
}

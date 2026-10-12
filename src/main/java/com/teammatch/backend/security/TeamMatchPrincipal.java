package com.teammatch.backend.security;

import com.teammatch.backend.entity.Student;
import com.teammatch.backend.entity.enums.AccountRole;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class TeamMatchPrincipal implements UserDetails {
	private final Long studentId;
	private final String username;
	private final String password;
	private final AccountRole role;

	public TeamMatchPrincipal(Student student) {
		this.studentId = student.getId();
		this.username = student.getEmail();
		this.password = student.getPasswordHash();
		this.role = student.getAccountRole();
	}

	public TeamMatchPrincipal(String username, String password) {
		this(username, password, null);
	}

	public TeamMatchPrincipal(String username, String password, AccountRole role) {
		this.studentId = null;
		this.username = username;
		this.password = password;
		this.role = role;
	}

	public Long getStudentId() {
		return studentId;
	}

	public AccountRole getRole() {
		return role;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return role == null ? List.of(new SimpleGrantedAuthority("ROLE_USER"))
				: List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return username;
	}
}

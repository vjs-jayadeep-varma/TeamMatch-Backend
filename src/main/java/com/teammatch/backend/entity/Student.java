package com.teammatch.backend.entity;

import com.teammatch.backend.entity.enums.AccountRole;
import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Entity;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "students", uniqueConstraints = {
		@UniqueConstraint(name = "uk_students_roll_number", columnNames = "roll_number"),
		@UniqueConstraint(name = "uk_students_email", columnNames = "email")
})
public class Student {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Size(max = 100)
	@Column(nullable = false, length = 100)
	private String name;

	@NotBlank
	@Size(max = 50)
	@Column(name = "roll_number", nullable = false, length = 50)
	private String rollNumber;

	@NotBlank
	@Email
	@Size(max = 254)
	@Column(nullable = false, length = 254)
	private String email;

	@NotBlank
	@Size(max = 255)
	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@NotBlank
	@Size(max = 100)
	@Column(nullable = false, length = 100)
	private String department;

	@NotNull
	@Min(1)
	@Max(8)
	@Column(name = "academic_year", nullable = false)
	private Integer academicYear;

	@Column(nullable = false)
	private boolean available = true;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "account_role", nullable = false, length = 20)
	private AccountRole accountRole = AccountRole.STUDENT;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "student_interests", joinColumns = @JoinColumn(name = "student_id"),
			uniqueConstraints = @UniqueConstraint(name = "uk_student_interests_student_interest",
					columnNames = { "student_id", "interest" }))
	@Column(name = "interest", nullable = false, length = 100)
	private Set<String> interests = new LinkedHashSet<>();

	protected Student() {
	}

	public Student(String name, String rollNumber, String email, String passwordHash, String department,
			Integer academicYear) {
		this.name = name;
		this.rollNumber = rollNumber;
		this.email = email;
		this.passwordHash = passwordHash;
		this.department = department;
		this.academicYear = academicYear;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRollNumber() {
		return rollNumber;
	}

	public void setRollNumber(String rollNumber) {
		this.rollNumber = rollNumber;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

	public Integer getAcademicYear() {
		return academicYear;
	}

	public void setAcademicYear(Integer academicYear) {
		this.academicYear = academicYear;
	}

	public boolean isAvailable() {
		return available;
	}

	public void setAvailable(boolean available) {
		this.available = available;
	}

	public AccountRole getAccountRole() {
		return accountRole;
	}

	public void setAccountRole(AccountRole accountRole) {
		this.accountRole = accountRole;
	}

	public Set<String> getInterests() {
		return interests;
	}

	public void setInterests(Set<String> interests) {
		this.interests = new LinkedHashSet<>(interests);
	}
}
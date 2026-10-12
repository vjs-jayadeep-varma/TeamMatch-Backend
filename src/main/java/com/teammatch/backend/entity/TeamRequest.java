package com.teammatch.backend.entity;

import com.teammatch.backend.entity.enums.TeamRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Entity
@Table(name = "team_requests")
public class TeamRequest {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "requesting_student_id", nullable = false)
	private Student requestingStudent;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "recipient_student_id", nullable = false)
	private Student recipientStudent;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "team_id", nullable = false)
	private Team requestedTeam;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TeamRequestStatus status = TeamRequestStatus.PENDING;

	@NotNull
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@NotNull
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected TeamRequest() {
	}

	public TeamRequest(Student requestingStudent, Student recipientStudent, Team requestedTeam) {
		this.requestingStudent = requestingStudent;
		this.recipientStudent = recipientStudent;
		this.requestedTeam = requestedTeam;
	}

	@PrePersist
	void beforeInsert() {
		Instant now = Instant.now();
		if (createdAt == null) {
			createdAt = now;
		}
		updatedAt = now;
	}

	@PreUpdate
	void beforeUpdate() {
		updatedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public Student getRequestingStudent() {
		return requestingStudent;
	}

	public void setRequestingStudent(Student requestingStudent) {
		this.requestingStudent = requestingStudent;
	}

	public Student getRecipientStudent() {
		return recipientStudent;
	}

	public void setRecipientStudent(Student recipientStudent) {
		this.recipientStudent = recipientStudent;
	}

	public Team getRequestedTeam() {
		return requestedTeam;
	}

	public void setRequestedTeam(Team requestedTeam) {
		this.requestedTeam = requestedTeam;
	}

	public TeamRequestStatus getStatus() {
		return status;
	}

	public void setStatus(TeamRequestStatus status) {
		this.status = status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
ALTER TABLE team_requests
	ADD COLUMN recipient_student_id BIGINT NULL;

UPDATE team_requests request_record
JOIN teams team_record ON team_record.id = request_record.team_id
JOIN projects project_record ON project_record.id = team_record.project_id
SET request_record.recipient_student_id = project_record.owner_student_id
WHERE request_record.recipient_student_id IS NULL;

ALTER TABLE team_requests
	MODIFY COLUMN recipient_student_id BIGINT NOT NULL,
	ADD CONSTRAINT fk_team_requests_recipient_student
		FOREIGN KEY (recipient_student_id) REFERENCES students (id),
	ADD INDEX idx_team_requests_recipient_created (recipient_student_id, created_at);

CREATE TABLE student_interests (
	student_id BIGINT NOT NULL,
	interest VARCHAR(100) NOT NULL,
	CONSTRAINT pk_student_interests PRIMARY KEY (student_id, interest),
	CONSTRAINT fk_student_interests_student
		FOREIGN KEY (student_id) REFERENCES students (id)
);

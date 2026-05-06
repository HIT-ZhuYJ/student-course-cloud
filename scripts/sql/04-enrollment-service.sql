USE db_enrollment;

CREATE TABLE IF NOT EXISTS enrollment (
  enrollment_id BIGINT NOT NULL AUTO_INCREMENT,
  student_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (enrollment_id),
  UNIQUE KEY uk_enrollment_student_course (student_id, course_id),
  KEY idx_enrollment_student_id (student_id),
  KEY idx_enrollment_course_id (course_id),
  KEY idx_enrollment_status (status),
  CONSTRAINT chk_enrollment_status CHECK (status IN ('ACTIVE', 'DROPPED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

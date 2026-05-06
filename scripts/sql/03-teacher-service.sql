USE db_teacher;

CREATE TABLE IF NOT EXISTS teacher (
  teacher_id BIGINT NOT NULL AUTO_INCREMENT,
  teacher_no VARCHAR(64) NOT NULL,
  name VARCHAR(64) NOT NULL,
  title VARCHAR(64) NULL,
  phone VARCHAR(32) NULL,
  email VARCHAR(128) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (teacher_id),
  UNIQUE KEY uk_teacher_teacher_no (teacher_no),
  KEY idx_teacher_status (status),
  CONSTRAINT chk_teacher_status CHECK (status IN ('ACTIVE', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS teacher_course_assignment (
  id BIGINT NOT NULL AUTO_INCREMENT,
  teacher_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_teacher_course_assignment (teacher_id, course_id),
  KEY idx_teacher_course_assignment_course_id (course_id),
  KEY idx_teacher_course_assignment_status (status),
  CONSTRAINT chk_teacher_course_assignment_status CHECK (status IN ('ACTIVE', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

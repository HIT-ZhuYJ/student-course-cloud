USE db_course;

CREATE TABLE IF NOT EXISTS course (
  course_id BIGINT NOT NULL AUTO_INCREMENT,
  course_code VARCHAR(64) NOT NULL,
  course_name VARCHAR(128) NOT NULL,
  credit DECIMAL(3,1) NOT NULL,
  capacity INT NOT NULL,
  selected_count INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  description VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (course_id),
  UNIQUE KEY uk_course_course_code (course_code),
  KEY idx_course_status (status),
  CONSTRAINT chk_course_credit CHECK (credit > 0),
  CONSTRAINT chk_course_capacity CHECK (capacity > 0),
  CONSTRAINT chk_course_selected_count CHECK (selected_count >= 0 AND selected_count <= capacity),
  CONSTRAINT chk_course_status CHECK (status IN ('OPEN', 'CLOSED', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS course_schedule (
  schedule_id BIGINT NOT NULL AUTO_INCREMENT,
  course_id BIGINT NOT NULL,
  weekday TINYINT NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  classroom VARCHAR(128) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (schedule_id),
  KEY idx_course_schedule_course_id (course_id),
  KEY idx_course_schedule_time (weekday, start_time, end_time),
  CONSTRAINT chk_course_schedule_weekday CHECK (weekday BETWEEN 1 AND 7),
  CONSTRAINT chk_course_schedule_time CHECK (start_time < end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

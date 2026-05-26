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
  start_week TINYINT NOT NULL,
  end_week TINYINT NOT NULL,
  week_type VARCHAR(10) NOT NULL DEFAULT 'ALL',
  weekday TINYINT NOT NULL,
  start_section TINYINT NOT NULL,
  end_section TINYINT NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  classroom VARCHAR(128) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (schedule_id),
  KEY idx_course_schedule_course_id (course_id),
  KEY idx_course_schedule_time (weekday, start_week, end_week, start_time, end_time),
  CONSTRAINT chk_course_schedule_week_range CHECK (start_week BETWEEN 1 AND 30 AND end_week BETWEEN start_week AND 30),
  CONSTRAINT chk_course_schedule_week_type CHECK (week_type IN ('ALL', 'ODD', 'EVEN')),
  CONSTRAINT chk_course_schedule_weekday CHECK (weekday BETWEEN 1 AND 7),
  CONSTRAINT chk_course_schedule_section CHECK (start_section BETWEEN 1 AND 12 AND end_section BETWEEN start_section AND 12),
  CONSTRAINT chk_course_schedule_time CHECK (start_time < end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

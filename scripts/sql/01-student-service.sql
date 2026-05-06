USE db_student;

CREATE TABLE IF NOT EXISTS user_account (
  id BIGINT NOT NULL AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(20) NOT NULL,
  related_id BIGINT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_account_username (username),
  KEY idx_user_account_role (role),
  KEY idx_user_account_related_id (related_id),
  CONSTRAINT chk_user_account_role CHECK (role IN ('STUDENT', 'TEACHER', 'ADMIN')),
  CONSTRAINT chk_user_account_status CHECK (status IN ('ACTIVE', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS student (
  student_id BIGINT NOT NULL AUTO_INCREMENT,
  student_no VARCHAR(64) NOT NULL,
  name VARCHAR(64) NOT NULL,
  major VARCHAR(128) NOT NULL,
  grade VARCHAR(32) NOT NULL,
  phone VARCHAR(32) NULL,
  email VARCHAR(128) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (student_id),
  UNIQUE KEY uk_student_student_no (student_no),
  KEY idx_student_status (status),
  CONSTRAINT chk_student_status CHECK (status IN ('ACTIVE', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

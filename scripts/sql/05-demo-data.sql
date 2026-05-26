USE db_course;

CREATE TEMPORARY TABLE IF NOT EXISTS stale_course_ids (
  course_id BIGINT NOT NULL PRIMARY KEY
);

TRUNCATE TABLE stale_course_ids;

INSERT IGNORE INTO stale_course_ids (course_id)
SELECT c.course_id
FROM db_course.course c
WHERE NOT EXISTS (
  SELECT 1
  FROM db_course.course_schedule s
  WHERE s.course_id = c.course_id
);

INSERT IGNORE INTO stale_course_ids (course_id)
VALUES (1), (2), (3), (4), (5), (6), (7), (8);

DELETE e
FROM db_enrollment.enrollment e
JOIN stale_course_ids s ON s.course_id = e.course_id;

DELETE a
FROM db_teacher.teacher_course_assignment a
JOIN stale_course_ids s ON s.course_id = a.course_id;

DELETE cs
FROM db_course.course_schedule cs
JOIN stale_course_ids s ON s.course_id = cs.course_id;

DELETE c
FROM db_course.course c
JOIN stale_course_ids s ON s.course_id = c.course_id;

DROP TEMPORARY TABLE stale_course_ids;

USE db_student;

INSERT INTO student (
  student_id, student_no, name, major, grade, phone, email, status
) VALUES
  (1, 'S2026001', 'Li Ming', 'Computer Science', '2026', '13800000001', 'liming@example.com', 'ACTIVE'),
  (2, 'S2026002', 'Wang Fang', 'Software Engineering', '2026', '13800000002', 'wangfang@example.com', 'ACTIVE'),
  (3, 'S2026003', 'Zhao Yue', 'Artificial Intelligence', '2026', '13800000003', 'zhaoyue@example.com', 'ACTIVE')
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  major = VALUES(major),
  grade = VALUES(grade),
  phone = VALUES(phone),
  email = VALUES(email),
  status = VALUES(status);

INSERT INTO user_account (
  id, username, password_hash, role, related_id, status
) VALUES
  (1, 'admin', '$2b$10$OKKbl9RgFGJfVAufhbBBYOp6UwvHVn7GImtfapsztjHga6A0b7FIm', 'ADMIN', NULL, 'ACTIVE'),
  (2, 'student001', '$2b$10$zraRizrN4vVi5vY0ty5UAeoKMA7WIrHQNjfQbSR.iwaXwdtr5gjqW', 'STUDENT', 1, 'ACTIVE'),
  (3, 'teacher001', '$2b$10$dZOmr8xyu4aKqDS8ar2i/uXg1l6NZ3633MArtekyVOgL3AhWbccvW', 'TEACHER', 1, 'ACTIVE'),
  (4, 'student002', '$2b$10$zraRizrN4vVi5vY0ty5UAeoKMA7WIrHQNjfQbSR.iwaXwdtr5gjqW', 'STUDENT', 2, 'ACTIVE')
ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  role = VALUES(role),
  related_id = VALUES(related_id),
  status = VALUES(status);

INSERT INTO user_account (
  username, password_hash, role, related_id, status
) VALUES
  ('student003', '$2b$10$zraRizrN4vVi5vY0ty5UAeoKMA7WIrHQNjfQbSR.iwaXwdtr5gjqW', 'STUDENT', 3, 'ACTIVE'),
  ('teacher002', '$2b$10$dZOmr8xyu4aKqDS8ar2i/uXg1l6NZ3633MArtekyVOgL3AhWbccvW', 'TEACHER', 2, 'ACTIVE'),
  ('teacher003', '$2b$10$dZOmr8xyu4aKqDS8ar2i/uXg1l6NZ3633MArtekyVOgL3AhWbccvW', 'TEACHER', 3, 'ACTIVE'),
  ('teacher004', '$2b$10$dZOmr8xyu4aKqDS8ar2i/uXg1l6NZ3633MArtekyVOgL3AhWbccvW', 'TEACHER', 4, 'ACTIVE')
ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  role = VALUES(role),
  related_id = VALUES(related_id),
  status = VALUES(status);

USE db_teacher;

INSERT INTO teacher (
  teacher_id, teacher_no, name, title, phone, email, status
) VALUES
  (1, 'T2026001', 'Zhang Wei', 'Professor', '13900000001', 'zhangwei@example.com', 'ACTIVE'),
  (2, 'T2026002', 'Chen Yan', 'Associate Professor', '13900000002', 'chenyan@example.com', 'ACTIVE'),
  (3, 'T2026003', 'Liu Qiang', 'Lecturer', '13900000003', 'liuqiang@example.com', 'ACTIVE'),
  (4, 'T2026004', 'Sun Mei', 'Lecturer', '13900000004', 'sunmei@example.com', 'ACTIVE')
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  title = VALUES(title),
  phone = VALUES(phone),
  email = VALUES(email),
  status = VALUES(status);

USE db_course;

INSERT INTO course (
  course_id, course_code, course_name, credit, capacity, selected_count, status, description
) VALUES
  (1, 'CS101', 'Software Construction C', 3.0, 50, 0, 'OPEN', 'Java 17 and Spring Boot engineering practice.'),
  (2, 'CS102', 'Database Systems', 3.0, 45, 0, 'OPEN', 'Relational model, SQL, transactions, and indexing.'),
  (3, 'CS201', 'Service-Oriented Computing', 3.0, 40, 0, 'OPEN', 'Spring Cloud microservices and service governance.'),
  (4, 'MATH201', 'Discrete Mathematics', 3.0, 40, 0, 'OPEN', 'Sets, logic, graphs, and combinatorics.'),
  (5, 'ENG101', 'College English', 2.0, 60, 0, 'OPEN', 'Academic reading and speaking practice.'),
  (6, 'PE101', 'Physical Education', 1.0, 30, 0, 'OPEN', 'Basketball and endurance training.'),
  (7, 'CS301', 'Software Project Practice', 2.0, 35, 0, 'OPEN', 'Team project lab with staged delivery.'),
  (8, 'AI201', 'Introduction to Artificial Intelligence', 2.5, 35, 0, 'OPEN', 'Search, machine learning basics, and AI applications.')
ON DUPLICATE KEY UPDATE
  course_name = VALUES(course_name),
  credit = VALUES(credit),
  capacity = VALUES(capacity),
  selected_count = VALUES(selected_count),
  status = VALUES(status),
  description = VALUES(description);

INSERT INTO course_schedule (
  schedule_id, course_id, start_week, end_week, week_type, weekday, start_section, end_section, start_time, end_time, classroom
) VALUES
  (1, 1, 1, 16, 'ALL', 1, 1, 2, '08:00:00', '09:45:00', 'A101'),
  (2, 2, 1, 16, 'ALL', 2, 3, 4, '10:00:00', '11:45:00', 'B214'),
  (3, 3, 1, 8, 'ODD', 3, 5, 6, '13:45:00', '15:30:00', 'C311'),
  (4, 4, 1, 16, 'ALL', 4, 1, 2, '08:00:00', '09:45:00', 'A304'),
  (5, 5, 1, 16, 'ALL', 5, 3, 4, '10:00:00', '11:45:00', 'D102'),
  (6, 6, 7, 12, 'ALL', 5, 5, 6, '13:45:00', '15:30:00', 'Stadium-1'),
  (7, 7, 17, 18, 'ALL', 1, 5, 8, '13:45:00', '17:30:00', 'Lab208'),
  (8, 8, 3, 10, 'EVEN', 2, 9, 10, '18:30:00', '20:15:00', 'B417')
ON DUPLICATE KEY UPDATE
  course_id = VALUES(course_id),
  start_week = VALUES(start_week),
  end_week = VALUES(end_week),
  week_type = VALUES(week_type),
  weekday = VALUES(weekday),
  start_section = VALUES(start_section),
  end_section = VALUES(end_section),
  start_time = VALUES(start_time),
  end_time = VALUES(end_time),
  classroom = VALUES(classroom);

USE db_teacher;

INSERT INTO teacher_course_assignment (
  id, teacher_id, course_id, status
) VALUES
  (1, 1, 1, 'ACTIVE'),
  (2, 2, 2, 'ACTIVE'),
  (3, 2, 3, 'ACTIVE'),
  (4, 3, 4, 'ACTIVE'),
  (5, 4, 5, 'ACTIVE'),
  (6, 4, 6, 'ACTIVE'),
  (7, 1, 7, 'ACTIVE'),
  (8, 3, 8, 'ACTIVE')
ON DUPLICATE KEY UPDATE
  teacher_id = VALUES(teacher_id),
  course_id = VALUES(course_id),
  status = VALUES(status);

USE db_enrollment;

INSERT INTO enrollment (
  enrollment_id, student_id, course_id, status
) VALUES
  (1, 1, 1, 'ACTIVE'),
  (2, 1, 2, 'ACTIVE'),
  (3, 1, 3, 'ACTIVE'),
  (4, 1, 5, 'ACTIVE'),
  (5, 2, 2, 'ACTIVE'),
  (6, 2, 4, 'ACTIVE'),
  (7, 2, 6, 'ACTIVE'),
  (8, 3, 5, 'ACTIVE'),
  (9, 3, 6, 'ACTIVE'),
  (10, 3, 8, 'ACTIVE')
ON DUPLICATE KEY UPDATE
  student_id = VALUES(student_id),
  course_id = VALUES(course_id),
  status = VALUES(status);

UPDATE db_course.course c
LEFT JOIN (
  SELECT course_id, COUNT(*) AS active_count
  FROM db_enrollment.enrollment
  WHERE status = 'ACTIVE'
    AND course_id BETWEEN 1 AND 8
  GROUP BY course_id
) e ON e.course_id = c.course_id
SET c.selected_count = COALESCE(e.active_count, 0)
WHERE c.course_id BETWEEN 1 AND 8;

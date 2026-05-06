USE db_student;

INSERT INTO student (
  student_id, student_no, name, major, grade, phone, email, status
) VALUES
  (1, 'S2026001', 'Li Ming', 'Computer Science', '2026', '13800000001', 'liming@example.com', 'ACTIVE'),
  (2, 'S2026002', 'Wang Fang', 'Software Engineering', '2026', '13800000002', 'wangfang@example.com', 'ACTIVE')
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

USE db_course;

INSERT INTO course (
  course_id, course_code, course_name, credit, capacity, selected_count, status, description
) VALUES
  (1, 'CS101', 'Java Programming', 3.0, 50, 1, 'OPEN', 'Java 17 and Spring Boot basics.'),
  (2, 'MATH201', 'Discrete Mathematics', 3.0, 40, 0, 'OPEN', 'Sets, logic, graphs, and combinatorics.'),
  (3, 'ENG101', 'College English', 2.0, 60, 0, 'OPEN', 'Academic reading and speaking practice.')
ON DUPLICATE KEY UPDATE
  course_name = VALUES(course_name),
  credit = VALUES(credit),
  capacity = VALUES(capacity),
  selected_count = VALUES(selected_count),
  status = VALUES(status),
  description = VALUES(description);

INSERT INTO course_schedule (
  schedule_id, course_id, weekday, start_time, end_time, classroom
) VALUES
  (1, 1, 1, '08:00:00', '09:40:00', 'A101'),
  (2, 2, 3, '10:00:00', '11:40:00', 'B202'),
  (3, 3, 5, '14:00:00', '15:40:00', 'C303')
ON DUPLICATE KEY UPDATE
  course_id = VALUES(course_id),
  weekday = VALUES(weekday),
  start_time = VALUES(start_time),
  end_time = VALUES(end_time),
  classroom = VALUES(classroom);

USE db_teacher;

INSERT INTO teacher (
  teacher_id, teacher_no, name, title, phone, email, status
) VALUES
  (1, 'T2026001', 'Zhang Wei', 'Professor', '13900000001', 'zhangwei@example.com', 'ACTIVE'),
  (2, 'T2026002', 'Chen Yan', 'Associate Professor', '13900000002', 'chenyan@example.com', 'ACTIVE')
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  title = VALUES(title),
  phone = VALUES(phone),
  email = VALUES(email),
  status = VALUES(status);

INSERT INTO teacher_course_assignment (
  id, teacher_id, course_id, status
) VALUES
  (1, 1, 1, 'ACTIVE'),
  (2, 2, 2, 'ACTIVE')
ON DUPLICATE KEY UPDATE
  teacher_id = VALUES(teacher_id),
  course_id = VALUES(course_id),
  status = VALUES(status);

USE db_enrollment;

INSERT INTO enrollment (
  enrollment_id, student_id, course_id, status
) VALUES
  (1, 1, 1, 'ACTIVE')
ON DUPLICATE KEY UPDATE
  student_id = VALUES(student_id),
  course_id = VALUES(course_id),
  status = VALUES(status);

USE db_course;

DROP PROCEDURE IF EXISTS add_course_schedule_column_if_missing;

DELIMITER //
CREATE PROCEDURE add_course_schedule_column_if_missing(
  IN column_name_to_add VARCHAR(64),
  IN column_definition TEXT,
  IN after_column VARCHAR(64)
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'course_schedule'
      AND column_name = column_name_to_add
  ) THEN
    SET @ddl = CONCAT(
      'ALTER TABLE course_schedule ADD COLUMN ',
      column_name_to_add,
      ' ',
      column_definition,
      ' AFTER ',
      after_column
    );
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END//
DELIMITER ;

CALL add_course_schedule_column_if_missing('start_week', 'TINYINT NOT NULL DEFAULT 1', 'course_id');
CALL add_course_schedule_column_if_missing('end_week', 'TINYINT NOT NULL DEFAULT 16', 'start_week');
CALL add_course_schedule_column_if_missing('week_type', 'VARCHAR(10) NOT NULL DEFAULT ''ALL''', 'end_week');
CALL add_course_schedule_column_if_missing('start_section', 'TINYINT NOT NULL DEFAULT 1', 'weekday');
CALL add_course_schedule_column_if_missing('end_section', 'TINYINT NOT NULL DEFAULT 2', 'start_section');

DROP PROCEDURE IF EXISTS add_course_schedule_column_if_missing;

UPDATE course_schedule
SET
  start_section = CASE
    WHEN start_time >= '20:30:00' THEN 11
    WHEN start_time >= '18:30:00' THEN 9
    WHEN start_time >= '15:45:00' THEN 7
    WHEN start_time >= '13:45:00' THEN 5
    WHEN start_time >= '10:00:00' THEN 3
    ELSE 1
  END,
  end_section = CASE
    WHEN end_time <= '09:45:00' THEN 2
    WHEN end_time <= '11:45:00' THEN 4
    WHEN end_time <= '15:30:00' THEN 6
    WHEN end_time <= '17:30:00' THEN 8
    WHEN end_time <= '20:15:00' THEN 10
    ELSE 12
  END
WHERE start_section = 1
  AND end_section = 2
  AND NOT (start_time <=> '08:00:00' AND end_time <= '09:45:00');

SET @idx_exists = (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'course_schedule'
    AND index_name = 'idx_course_schedule_week_time'
);

SET @ddl = IF(
  @idx_exists = 0,
  'ALTER TABLE course_schedule ADD KEY idx_course_schedule_week_time (weekday, start_week, end_week, start_time, end_time)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

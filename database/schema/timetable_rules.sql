-- Run once in MySQL Workbench after database_registration_rules.sql has been applied.
USE student_registration;

ALTER TABLE courses
  ADD COLUMN periods_per_week TINYINT UNSIGNED NOT NULL DEFAULT 1,
  ADD CONSTRAINT chk_courses_periods_per_week CHECK (periods_per_week BETWEEN 1 AND 8);

UPDATE courses SET periods_per_week = CASE course_id
  WHEN 1 THEN 5
  WHEN 2 THEN 7
  WHEN 3 THEN 5
  WHEN 4 THEN 5
  WHEN 5 THEN 4
  WHEN 6 THEN 5
  WHEN 7 THEN 5
  WHEN 8 THEN 5
  WHEN 9 THEN 5
  WHEN 10 THEN 4
  WHEN 11 THEN 5
  WHEN 12 THEN 7
  ELSE periods_per_week
END
WHERE course_id BETWEEN 1 AND 12
  AND department = 'CSBS'
  AND academic_year = '2026-2027'
  AND semester = 3;

ALTER TABLE timetable_entries
  DROP INDEX uq_timetable_student_course,
  ADD UNIQUE KEY uq_timetable_student_course_period
    (student_id, course_id, day_of_week, start_time);

SELECT course_id, course_code, periods_per_week
FROM courses
WHERE department = 'CSBS' AND academic_year = '2026-2027' AND semester = 3
ORDER BY course_id;

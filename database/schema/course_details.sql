-- Course types and syllabus units/topics for CSBS Semester 3, academic year 2026-2027.
-- Safe to run again: existing course types are retained while units are refreshed.
USE student_registration;
SET @course_type_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'courses' AND column_name = 'course_type'
);
SET @course_type_ddl = IF(
    @course_type_exists = 0,
    'ALTER TABLE courses ADD COLUMN course_type VARCHAR(20) NOT NULL DEFAULT ''THEORY''',
    'SELECT ''courses.course_type already exists'' AS migration_status'
);
PREPARE course_type_stmt FROM @course_type_ddl;
EXECUTE course_type_stmt;
DEALLOCATE PREPARE course_type_stmt;
CREATE TABLE IF NOT EXISTS course_units (
    unit_id BIGINT NOT NULL AUTO_INCREMENT,
    course_id BIGINT UNSIGNED NOT NULL,
    unit_number TINYINT UNSIGNED NOT NULL,
    unit_name VARCHAR(160) NOT NULL,
    topics JSON NOT NULL,
    PRIMARY KEY (unit_id),
    UNIQUE KEY uq_course_units_course_number (course_id, unit_number),
    CONSTRAINT fk_course_units_course FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE
);
UPDATE courses SET course_type = CASE course_code
    WHEN 'CS3301' THEN 'PRACTICAL'
    WHEN 'CS3302' THEN 'PRACTICAL'
    WHEN 'CS3303' THEN 'PRACTICAL'
    WHEN 'CS3304' THEN 'PRACTICAL'
    WHEN 'CS3305' THEN 'THEORY'
    WHEN 'CS3306' THEN 'PRACTICAL'
    WHEN 'BS3301' THEN 'THEORY'
    WHEN 'BS3302' THEN 'THEORY'
    WHEN 'BS3303' THEN 'PRACTICAL'
    WHEN 'CS3307' THEN 'PRACTICAL'
    WHEN 'CS3308' THEN 'THEORY'
    WHEN 'CS3309' THEN 'PRACTICAL'
    ELSE course_type
END
WHERE course_id IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
DELETE FROM course_units
WHERE course_id IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
-- Verify each course type and confirm that each course has five units.
SELECT course_code, course_type FROM courses WHERE course_code IN ('CS3301', 'CS3302', 'CS3303', 'CS3304', 'CS3305', 'CS3306', 'BS3301', 'BS3302', 'BS3303', 'CS3307', 'CS3308', 'CS3309') ORDER BY course_id;
SELECT c.course_code, COUNT(*) AS unit_count FROM course_units u JOIN courses c ON c.course_id = u.course_id GROUP BY c.course_code ORDER BY c.course_code;

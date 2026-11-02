-- Additive migration for the Semester 3 registration deadline and default courses.
-- Run this once in MySQL Workbench after selecting the student_registration schema.
USE student_registration;

CREATE TABLE IF NOT EXISTS registration_policies (
  policy_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  department VARCHAR(80) NOT NULL,
  academic_year VARCHAR(9) NOT NULL,
  semester TINYINT UNSIGNED NOT NULL,
  deadline_at DATETIME NOT NULL,
  processed_at DATETIME NULL,
  PRIMARY KEY (policy_id),
  UNIQUE KEY uq_registration_policy_term (department, academic_year, semester),
  CONSTRAINT chk_registration_policy_semester CHECK (semester BETWEEN 1 AND 12)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS registration_default_courses (
  policy_id BIGINT UNSIGNED NOT NULL,
  course_id BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (policy_id, course_id),
  CONSTRAINT fk_registration_defaults_policy FOREIGN KEY (policy_id)
    REFERENCES registration_policies (policy_id) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_registration_defaults_course FOREIGN KEY (course_id)
    REFERENCES courses (course_id) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

DELETE d FROM registration_default_courses d
JOIN registration_policies p ON p.policy_id = d.policy_id
WHERE p.department = 'CSBS' AND p.academic_year = '2026-2027' AND p.semester = 3;

-- This result must be 6 before the backend can apply the default allotment.
SELECT p.policy_id, p.department, p.academic_year, p.semester, p.deadline_at,
       COUNT(c.course_id) AS valid_default_courses
FROM registration_policies p
LEFT JOIN registration_default_courses d ON d.policy_id = p.policy_id
LEFT JOIN courses c ON c.course_id = d.course_id
  AND c.department = p.department AND c.academic_year = p.academic_year
  AND c.semester = p.semester
WHERE p.department = 'CSBS' AND p.academic_year = '2026-2027' AND p.semester = 3
GROUP BY p.policy_id, p.department, p.academic_year, p.semester, p.deadline_at;


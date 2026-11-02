-- Student Course Registration and Timetable Generator
-- MySQL 8.0+
-- Run this script in MySQL Workbench or with: mysql -u root -p < database.sql

CREATE DATABASE IF NOT EXISTS student_registration
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE student_registration;

CREATE TABLE users (
  user_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role ENUM('STUDENT', 'FACULTY', 'ADMIN') NOT NULL DEFAULT 'STUDENT',
  email VARCHAR(254) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id),
  UNIQUE KEY uq_users_username (username),
  UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB;

CREATE TABLE students (
  student_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  register_number VARCHAR(30) NOT NULL,
  student_name VARCHAR(120) NOT NULL,
  department VARCHAR(80) NOT NULL,
  academic_year VARCHAR(9) NOT NULL,
  current_semester TINYINT UNSIGNED NOT NULL,
  PRIMARY KEY (student_id),
  UNIQUE KEY uq_students_user (user_id),
  UNIQUE KEY uq_students_register_number (register_number),
  CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users (user_id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT chk_students_semester CHECK (current_semester BETWEEN 1 AND 12)
) ENGINE=InnoDB;

CREATE TABLE faculty (
  faculty_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  faculty_name VARCHAR(120) NOT NULL,
  department VARCHAR(80) NOT NULL,
  PRIMARY KEY (faculty_id),
  UNIQUE KEY uq_faculty_user (user_id),
  CONSTRAINT fk_faculty_user FOREIGN KEY (user_id) REFERENCES users (user_id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

-- Each row represents a course offered in a particular academic year and semester.
CREATE TABLE courses (
  course_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  course_code VARCHAR(20) NOT NULL,
  course_name VARCHAR(150) NOT NULL,
  faculty_id BIGINT UNSIGNED NOT NULL,
  department VARCHAR(80) NOT NULL,
  semester TINYINT UNSIGNED NOT NULL,
  academic_year VARCHAR(9) NOT NULL,
  credits TINYINT UNSIGNED NOT NULL,
  PRIMARY KEY (course_id),
  UNIQUE KEY uq_courses_term_code (department, academic_year, semester, course_code),
  KEY ix_courses_term (department, academic_year, semester),
  CONSTRAINT fk_courses_faculty FOREIGN KEY (faculty_id) REFERENCES faculty (faculty_id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT chk_courses_semester CHECK (semester BETWEEN 1 AND 12),
  CONSTRAINT chk_courses_credits CHECK (credits BETWEEN 1 AND 30)
) ENGINE=InnoDB;

CREATE TABLE registrations (
  registration_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  student_id BIGINT UNSIGNED NOT NULL,
  course_id BIGINT UNSIGNED NOT NULL,
  registered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (registration_id),
  UNIQUE KEY uq_registrations_student_course (student_id, course_id),
  KEY ix_registrations_course (course_id),
  CONSTRAINT fk_registrations_student FOREIGN KEY (student_id) REFERENCES students (student_id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_registrations_course FOREIGN KEY (course_id) REFERENCES courses (course_id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE faculty_availability (
  availability_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  faculty_id BIGINT UNSIGNED NOT NULL,
  day_of_week ENUM('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY') NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  is_available BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (availability_id),
  UNIQUE KEY uq_faculty_availability_slot (faculty_id, day_of_week, start_time, end_time),
  CONSTRAINT fk_availability_faculty FOREIGN KEY (faculty_id) REFERENCES faculty (faculty_id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT chk_availability_time CHECK (end_time > start_time)
) ENGINE=InnoDB;

-- A row is one student's scheduled class. The application should save a generated timetable
-- in one transaction and validate student/faculty time conflicts before inserting it.
CREATE TABLE timetable_entries (
  timetable_entry_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  student_id BIGINT UNSIGNED NOT NULL,
  course_id BIGINT UNSIGNED NOT NULL,
  faculty_id BIGINT UNSIGNED NOT NULL,
  day_of_week ENUM('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY') NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (timetable_entry_id),
  UNIQUE KEY uq_timetable_student_course (student_id, course_id),
  KEY ix_timetable_student_slot (student_id, day_of_week, start_time),
  KEY ix_timetable_faculty_slot (faculty_id, day_of_week, start_time),
  CONSTRAINT fk_timetable_student FOREIGN KEY (student_id) REFERENCES students (student_id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_timetable_course FOREIGN KEY (course_id) REFERENCES courses (course_id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_timetable_faculty FOREIGN KEY (faculty_id) REFERENCES faculty (faculty_id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT chk_timetable_time CHECK (end_time > start_time)
) ENGINE=InnoDB;
USE student_registration;

SELECT user_id, username, password_hash, role, email, created_at
FROM users;
SELECT * FROM users
WHERE username IN ('srija', 'kumar');


USE student_registration;
USE student_registration;

USE student_registration;

SELECT role, COUNT(*) FROM users GROUP BY role;
  SELECT user_id, username, role
FROM users
WHERE role = 'STUDENT';
USE student_registration;

USE student_registration;

SELECT s.register_number, s.student_name, u.username
FROM students s JOIN users u ON u.user_id = s.user_id;
USE student_registration;

SELECT f.faculty_name, u.username, u.role
FROM faculty f JOIN users u ON u.user_id = f.user_id;
USE student_registration;

USE student_registration;

USE student_registration;

USE student_registration;


USE student_registration;

CREATE TABLE IF NOT EXISTS attendance_records (
    attendance_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    timetable_entry_id BIGINT UNSIGNED NOT NULL,
    attendance_date DATE NOT NULL,
    status ENUM('PRESENT', 'ABSENT') NOT NULL,
    marked_by BIGINT UNSIGNED NOT NULL,
    marked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (attendance_id),
    UNIQUE KEY uq_attendance_entry_date (timetable_entry_id, attendance_date),
    KEY idx_attendance_faculty_date (marked_by, attendance_date),
    CONSTRAINT fk_attendance_timetable_entry
        FOREIGN KEY (timetable_entry_id)
        REFERENCES timetable_entries (timetable_entry_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_attendance_marked_by
        FOREIGN KEY (marked_by)
        REFERENCES faculty (faculty_id)
        ON DELETE RESTRICT
) ENGINE=InnoDB;

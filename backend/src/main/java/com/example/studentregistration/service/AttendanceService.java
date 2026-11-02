package com.example.studentregistration.service;

import com.example.studentregistration.dto.AttendanceBatchRequest;
import com.example.studentregistration.dto.AttendanceMarkRequest;
import com.example.studentregistration.dto.AttendanceSaveResponse;
import com.example.studentregistration.dto.FacultyAttendanceEntryResponse;
import com.example.studentregistration.dto.StudentAttendanceEntryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class AttendanceService {
    private final JdbcTemplate jdbcTemplate;

    public AttendanceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<FacultyAttendanceEntryResponse> getFacultyAttendance(Long facultyId, LocalDate date) {
        LocalDate attendanceDate = date == null ? LocalDate.now() : date;
        return jdbcTemplate.query(
                "SELECT t.timetable_entry_id, t.student_id, s.student_name, s.register_number, "
                        + "c.course_id, c.course_code, c.course_name, t.day_of_week, t.start_time, t.end_time, "
                        + "ar.status FROM timetable_entries t "
                        + "JOIN students s ON s.student_id = t.student_id "
                        + "JOIN courses c ON c.course_id = t.course_id "
                        + "LEFT JOIN attendance_records ar ON ar.timetable_entry_id = t.timetable_entry_id "
                        + "AND ar.attendance_date = ? "
                        + "WHERE t.faculty_id = ? AND t.day_of_week = ? "
                        + "ORDER BY t.start_time, c.course_code, s.student_name",
                (rs, rowNum) -> new FacultyAttendanceEntryResponse(
                        rs.getLong("timetable_entry_id"), rs.getLong("student_id"), rs.getString("student_name"),
                        rs.getString("register_number"), rs.getLong("course_id"), rs.getString("course_code"),
                        rs.getString("course_name"), rs.getString("day_of_week"), rs.getTime("start_time").toLocalTime(),
                        rs.getTime("end_time").toLocalTime(), attendanceDate, rs.getString("status")),
                java.sql.Date.valueOf(attendanceDate), facultyId, attendanceDate.getDayOfWeek().name());
    }

    public List<StudentAttendanceEntryResponse> getStudentAttendance(Long studentId, LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Choose an attendance date range of up to 31 days");
        }

        List<ScheduleEntry> schedule = jdbcTemplate.query(
                "SELECT t.timetable_entry_id, t.day_of_week, t.start_time, t.end_time, "
                        + "c.course_code, c.course_name, f.faculty_name "
                        + "FROM timetable_entries t JOIN courses c ON c.course_id = t.course_id "
                        + "JOIN faculty f ON f.faculty_id = t.faculty_id "
                        + "WHERE t.student_id = ? ORDER BY t.start_time, c.course_code",
                (rs, rowNum) -> new ScheduleEntry(
                        rs.getLong("timetable_entry_id"), rs.getString("day_of_week"),
                        rs.getTime("start_time").toLocalTime(), rs.getTime("end_time").toLocalTime(),
                        rs.getString("course_code"), rs.getString("course_name"), rs.getString("faculty_name")),
                studentId);
        if (schedule.isEmpty()) return List.of();

        String placeholders = String.join(",", java.util.Collections.nCopies(schedule.size(), "?"));
        List<Object> params = new ArrayList<>();
        params.add(java.sql.Date.valueOf(from));
        params.add(java.sql.Date.valueOf(to));
        schedule.forEach(entry -> params.add(entry.timetableEntryId()));
        Map<String, String> recordedStatuses = new HashMap<>();
        List<AttendanceRecord> attendance = jdbcTemplate.query(
                "SELECT timetable_entry_id, attendance_date, status FROM attendance_records "
                        + "WHERE attendance_date BETWEEN ? AND ? AND timetable_entry_id IN (" + placeholders + ")",
                (rs, rowNum) -> new AttendanceRecord(
                        rs.getLong("timetable_entry_id"), rs.getDate("attendance_date").toLocalDate(),
                        rs.getString("status")),
                params.toArray());
        attendance.forEach(record -> recordedStatuses.put(
                statusKey(record.timetableEntryId(), record.attendanceDate()), record.status()));

        List<StudentAttendanceEntryResponse> result = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            String day = date.getDayOfWeek().name();
            for (ScheduleEntry entry : schedule) {
                if (entry.dayOfWeek().equals(day)) {
                    result.add(new StudentAttendanceEntryResponse(
                            entry.timetableEntryId(), date, day, entry.startTime(), entry.endTime(),
                            entry.courseCode(), entry.courseName(), entry.facultyName(),
                            recordedStatuses.getOrDefault(statusKey(entry.timetableEntryId(), date), "NOT_MARKED")));
                }
            }
        }
        return result;
    }

    @Transactional
    public AttendanceSaveResponse markAttendance(Long facultyId, AttendanceBatchRequest request) {
        if (request == null || request.attendanceDate() == null || request.entries() == null
                || request.entries().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a class and mark its students");
        }
        LocalDate date = request.attendanceDate();
        if (date.isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attendance cannot be marked for a future date");
        }

        Set<Long> seenEntryIds = new HashSet<>();
        for (AttendanceMarkRequest entry : request.entries()) {
            if (entry == null || entry.timetableEntryId() == null || entry.status() == null
                    || !Set.of("PRESENT", "ABSENT").contains(entry.status().toUpperCase(Locale.ROOT))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each student must be marked present or absent");
            }
            if (!seenEntryIds.add(entry.timetableEntryId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A student period was included more than once");
            }
            Long ownedEntry = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM timetable_entries WHERE timetable_entry_id = ? AND faculty_id = ? AND day_of_week = ?",
                    Long.class, entry.timetableEntryId(), facultyId, date.getDayOfWeek().name());
            if (ownedEntry == null || ownedEntry != 1) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You can only mark attendance for a class on your timetable");
            }
        }

        for (AttendanceMarkRequest entry : request.entries()) {
            String status = entry.status().toUpperCase(Locale.ROOT);
            jdbcTemplate.update(
                    "INSERT INTO attendance_records (timetable_entry_id, attendance_date, status, marked_by) "
                            + "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE status = ?, marked_by = ?, marked_at = CURRENT_TIMESTAMP",
                    entry.timetableEntryId(), java.sql.Date.valueOf(date), status, facultyId, status, facultyId);
        }
        return new AttendanceSaveResponse(date, request.entries().size(), "Attendance saved successfully");
    }

    private static String statusKey(Long timetableEntryId, LocalDate date) {
        return timetableEntryId + ":" + date;
    }

    private record ScheduleEntry(Long timetableEntryId, String dayOfWeek, LocalTime startTime, LocalTime endTime,
                                 String courseCode, String courseName, String facultyName) {}
    private record AttendanceRecord(Long timetableEntryId, LocalDate attendanceDate, String status) {}
}

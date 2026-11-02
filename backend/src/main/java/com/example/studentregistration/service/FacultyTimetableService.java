package com.example.studentregistration.service;

import com.example.studentregistration.dto.FacultyClassResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FacultyTimetableService {
    private final JdbcTemplate jdbcTemplate;

    public FacultyTimetableService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<FacultyClassResponse> getWeeklyClasses(Long facultyId) {
        return jdbcTemplate.query(
                "SELECT c.course_id, c.course_code, c.course_name, c.academic_year, c.semester, "
                        + "t.day_of_week, t.start_time, t.end_time, COUNT(DISTINCT t.student_id) AS student_count "
                        + "FROM timetable_entries t JOIN courses c ON c.course_id = t.course_id "
                        + "WHERE t.faculty_id = ? "
                        + "GROUP BY c.course_id, c.course_code, c.course_name, c.academic_year, c.semester, "
                        + "t.day_of_week, t.start_time, t.end_time "
                        + "ORDER BY FIELD(t.day_of_week,'MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY'), "
                        + "t.start_time, c.course_code",
                (rs, rowNum) -> new FacultyClassResponse(
                        rs.getLong("course_id"), rs.getString("course_code"), rs.getString("course_name"),
                        rs.getString("academic_year"), rs.getInt("semester"), rs.getString("day_of_week"),
                        rs.getTime("start_time").toLocalTime(), rs.getTime("end_time").toLocalTime(),
                        rs.getInt("student_count")),
                facultyId);
    }
}

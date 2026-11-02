package com.example.studentregistration.service;

import com.example.studentregistration.dto.FacultyAvailabilityRequest;
import com.example.studentregistration.dto.FacultyAvailabilityResponse;
import com.example.studentregistration.repository.FacultyRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class FacultyAvailabilityService {
    private static final Set<String> DAYS = Set.of(
            "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
    );
    private static final String SELECT_COLUMNS =
            "SELECT availability_id, faculty_id, day_of_week, start_time, end_time, is_available "
                    + "FROM faculty_availability ";

    private final JdbcTemplate jdbcTemplate;
    private final FacultyRepository facultyRepository;

    public FacultyAvailabilityService(JdbcTemplate jdbcTemplate, FacultyRepository facultyRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.facultyRepository = facultyRepository;
    }

    public List<FacultyAvailabilityResponse> list(Long facultyId) {
        requireFaculty(facultyId);
        return jdbcTemplate.query(
                SELECT_COLUMNS + "WHERE faculty_id = ? ORDER BY day_of_week, start_time",
                (rs, rowNum) -> new FacultyAvailabilityResponse(
                        rs.getLong("availability_id"),
                        rs.getLong("faculty_id"),
                        rs.getString("day_of_week"),
                        rs.getTime("start_time").toLocalTime(),
                        rs.getTime("end_time").toLocalTime(),
                        rs.getBoolean("is_available")
                ),
                facultyId
        );
    }

    public FacultyAvailabilityResponse create(Long facultyId, FacultyAvailabilityRequest request) {
        requireFaculty(facultyId);
        String day = validate(request);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO faculty_availability "
                                + "(faculty_id, day_of_week, start_time, end_time, is_available) "
                                + "VALUES (?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                statement.setLong(1, facultyId);
                statement.setString(2, day);
                statement.setTime(3, Time.valueOf(request.startTime()));
                statement.setTime(4, Time.valueOf(request.endTime()));
                statement.setBoolean(5, request.isAvailable() == null || request.isAvailable());
                return statement;
            }, keyHolder);
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This availability slot already exists");
        }
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("MySQL did not return the new availability ID");
        }
        return findById(key.longValue());
    }

    public FacultyAvailabilityResponse update(Long availabilityId, FacultyAvailabilityRequest request) {
        String day = validate(request);
        try {
            int changed = jdbcTemplate.update(
                    "UPDATE faculty_availability SET day_of_week = ?, start_time = ?, end_time = ?, "
                            + "is_available = ? WHERE availability_id = ?",
                    day,
                    Time.valueOf(request.startTime()),
                    Time.valueOf(request.endTime()),
                    request.isAvailable() == null || request.isAvailable(),
                    availabilityId
            );
            if (changed == 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Availability slot not found");
            }
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This availability slot already exists");
        }
        return findById(availabilityId);
    }

    public void delete(Long availabilityId) {
        int changed = jdbcTemplate.update(
                "DELETE FROM faculty_availability WHERE availability_id = ?", availabilityId
        );
        if (changed == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Availability slot not found");
        }
    }

    private FacultyAvailabilityResponse findById(Long availabilityId) {
        return jdbcTemplate.queryForObject(
                SELECT_COLUMNS + "WHERE availability_id = ?",
                (rs, rowNum) -> new FacultyAvailabilityResponse(
                        rs.getLong("availability_id"),
                        rs.getLong("faculty_id"),
                        rs.getString("day_of_week"),
                        rs.getTime("start_time").toLocalTime(),
                        rs.getTime("end_time").toLocalTime(),
                        rs.getBoolean("is_available")
                ),
                availabilityId
        );
    }

    private void requireFaculty(Long facultyId) {
        if (facultyId == null || !facultyRepository.existsById(facultyId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Faculty member not found");
        }
    }

    private String validate(FacultyAvailabilityRequest request) {
        if (request == null || request.dayOfWeek() == null
                || request.startTime() == null || request.endTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Day and start/end times are required");
        }
        String day = request.dayOfWeek().trim().toUpperCase(Locale.ROOT);
        if (!DAYS.contains(day)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dayOfWeek must be a valid weekday name");
        }
        LocalTime start = request.startTime();
        LocalTime end = request.endTime();
        if (!end.isAfter(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endTime must be later than startTime");
        }
        return day;
    }
}
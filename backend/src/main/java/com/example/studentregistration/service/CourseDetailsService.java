package com.example.studentregistration.service;

import com.example.studentregistration.dto.CourseDetailsResponse;
import com.example.studentregistration.model.Course;
import com.example.studentregistration.repository.CourseRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CourseDetailsService {
    private static final TypeReference<List<String>> TOPIC_LIST = new TypeReference<>() {};

    private final CourseRepository courseRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public CourseDetailsService(CourseRepository courseRepository, JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.courseRepository = courseRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public CourseDetailsResponse getDetails(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        List<CourseDetailsResponse.Unit> units = jdbcTemplate.query(
                "SELECT unit_number, unit_name, topics FROM course_units WHERE course_id = ? ORDER BY unit_number",
                (rs, rowNum) -> new CourseDetailsResponse.Unit(
                        rs.getInt("unit_number"), rs.getString("unit_name"), parseTopics(rs.getString("topics"))),
                courseId);
        return new CourseDetailsResponse(course.getCourseId(), course.getCourseCode(), course.getCourseName(),
                course.getCourseType(), units);
    }

    private List<String> parseTopics(String json) {
        try {
            return objectMapper.readValue(json, TOPIC_LIST);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Invalid topics JSON stored for a course unit", exception);
        }
    }
}

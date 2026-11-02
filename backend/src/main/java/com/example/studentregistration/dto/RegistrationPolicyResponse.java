package com.example.studentregistration.dto;

import java.time.LocalDateTime;
import java.util.List;

public record RegistrationPolicyResponse(
        String department,
        String academicYear,
        int semester,
        LocalDateTime deadlineAt,
        boolean open,
        List<DefaultCourse> defaultCourses
) {
    public record DefaultCourse(String courseCode, String courseName) {}
}

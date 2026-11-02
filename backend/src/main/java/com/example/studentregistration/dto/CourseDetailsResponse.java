package com.example.studentregistration.dto;

import java.util.List;

public record CourseDetailsResponse(
        Long courseId,
        String courseCode,
        String courseName,
        String courseType,
        List<Unit> units
) {
    public record Unit(int unitNumber, String unitName, List<String> topics) {}
}

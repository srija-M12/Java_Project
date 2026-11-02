package com.example.studentregistration.dto;

import java.time.LocalTime;

public record FacultyClassResponse(
        Long courseId,
        String courseCode,
        String courseName,
        String academicYear,
        Integer semester,
        String dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        int studentCount
) {}

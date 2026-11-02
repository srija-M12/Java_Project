package com.example.studentregistration.dto;

import java.time.LocalTime;

public record TimetableEntryResponse(
        Long timetableEntryId,
        Long studentId,
        Long courseId,
        String courseCode,
        String courseName,
        Long facultyId,
        String facultyName,
        String dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
}
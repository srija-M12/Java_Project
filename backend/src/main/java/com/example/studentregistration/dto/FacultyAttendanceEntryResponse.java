package com.example.studentregistration.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record FacultyAttendanceEntryResponse(
        Long timetableEntryId,
        Long studentId,
        String studentName,
        String registerNumber,
        Long courseId,
        String courseCode,
        String courseName,
        String dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        LocalDate attendanceDate,
        String status
) {}

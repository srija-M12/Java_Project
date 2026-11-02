package com.example.studentregistration.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record StudentAttendanceEntryResponse(
        Long timetableEntryId,
        LocalDate attendanceDate,
        String dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        String courseCode,
        String courseName,
        String facultyName,
        String status
) {}

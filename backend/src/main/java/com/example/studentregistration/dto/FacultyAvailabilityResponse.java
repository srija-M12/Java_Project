package com.example.studentregistration.dto;

import java.time.LocalTime;

public record FacultyAvailabilityResponse(
        Long availabilityId,
        Long facultyId,
        String dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Boolean isAvailable
) {
}
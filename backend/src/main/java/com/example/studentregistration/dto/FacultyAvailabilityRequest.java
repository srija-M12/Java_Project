package com.example.studentregistration.dto;

import java.time.LocalTime;

public record FacultyAvailabilityRequest(
        String dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Boolean isAvailable
) {
}
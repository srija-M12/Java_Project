package com.example.studentregistration.dto;

public record StudentSignupRequest(
        String username,
        String email,
        String password,
        String campusCode,
        String studentName,
        String registerNumber,
        String department,
        String academicYear,
        Integer currentSemester
) {}

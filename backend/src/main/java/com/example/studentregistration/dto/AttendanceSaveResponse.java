package com.example.studentregistration.dto;

import java.time.LocalDate;

public record AttendanceSaveResponse(LocalDate attendanceDate, int updatedRecords, String message) {}

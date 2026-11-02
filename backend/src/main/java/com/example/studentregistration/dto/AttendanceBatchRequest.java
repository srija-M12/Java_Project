package com.example.studentregistration.dto;

import java.time.LocalDate;
import java.util.List;

public record AttendanceBatchRequest(LocalDate attendanceDate, List<AttendanceMarkRequest> entries) {}

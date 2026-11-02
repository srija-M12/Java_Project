package com.example.studentregistration.dto;

import java.util.List;

public record RegistrationResponse(Long studentId, List<Long> courseIds, String message) {
}
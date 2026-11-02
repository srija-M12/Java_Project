package com.example.studentregistration.dto;

public record LoginResponse(Long userId, String username, String email, String role) {
}
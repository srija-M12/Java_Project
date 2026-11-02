package com.example.studentregistration.dto;

import java.util.List;

public record RegisterCoursesRequest(Long studentId, List<Long> courseIds) {
}
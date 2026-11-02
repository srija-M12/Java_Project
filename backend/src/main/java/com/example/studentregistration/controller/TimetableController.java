package com.example.studentregistration.controller;

import com.example.studentregistration.dto.TimetableEntryResponse;
import com.example.studentregistration.service.TimetableService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/timetables")
public class TimetableController {
    private final TimetableService timetableService;

    public TimetableController(TimetableService timetableService) {
        this.timetableService = timetableService;
    }

    @GetMapping("/{studentId}")
    public List<TimetableEntryResponse> getTimetable(@PathVariable Long studentId) {
        return timetableService.getTimetable(studentId);
    }

    @PostMapping("/generate/{studentId}")
    @ResponseStatus(HttpStatus.CREATED)
    public List<TimetableEntryResponse> generate(@PathVariable Long studentId) {
        return timetableService.generate(studentId);
    }
}
package com.example.studentregistration.controller;

import com.example.studentregistration.dto.FacultyAvailabilityRequest;
import com.example.studentregistration.dto.FacultyAvailabilityResponse;
import com.example.studentregistration.dto.FacultyClassResponse;
import com.example.studentregistration.model.Faculty;
import com.example.studentregistration.repository.FacultyRepository;
import com.example.studentregistration.service.FacultyAvailabilityService;
import com.example.studentregistration.service.FacultyTimetableService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/faculty")
public class FacultyController {
    private final FacultyRepository facultyRepository;
    private final FacultyAvailabilityService availabilityService;
    private final FacultyTimetableService timetableService;

    public FacultyController(FacultyRepository facultyRepository, FacultyAvailabilityService availabilityService,
                             FacultyTimetableService timetableService) {
        this.facultyRepository = facultyRepository;
        this.availabilityService = availabilityService;
        this.timetableService = timetableService;
    }

    @GetMapping("/me")
    public Faculty getMyFacultyProfile(HttpSession session) {
        Object userId = session.getAttribute("appUserId");
        if (!(userId instanceof Number number)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in first");
        }
        return facultyRepository.findByUserId(number.longValue())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No faculty profile is linked to this account"));
    }

    @GetMapping("/me/timetable")
    public List<FacultyClassResponse> getMyTimetable(HttpSession session) {
        Faculty faculty = getMyFacultyProfile(session);
        return timetableService.getWeeklyClasses(faculty.getFacultyId());
    }

    @GetMapping("/{facultyId}")
    public Faculty getFaculty(@PathVariable Long facultyId) {
        return facultyRepository.findById(facultyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Faculty member not found"));
    }

    @GetMapping("/{facultyId}/availability")
    public List<FacultyAvailabilityResponse> getAvailability(@PathVariable Long facultyId) {
        return availabilityService.list(facultyId);
    }

    @PostMapping("/{facultyId}/availability")
    @ResponseStatus(HttpStatus.CREATED)
    public FacultyAvailabilityResponse addAvailability(
            @PathVariable Long facultyId, @RequestBody FacultyAvailabilityRequest request) {
        return availabilityService.create(facultyId, request);
    }

    @PutMapping("/availability/{availabilityId}")
    public FacultyAvailabilityResponse updateAvailability(
            @PathVariable Long availabilityId, @RequestBody FacultyAvailabilityRequest request) {
        return availabilityService.update(availabilityId, request);
    }

    @DeleteMapping("/availability/{availabilityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAvailability(@PathVariable Long availabilityId) {
        availabilityService.delete(availabilityId);
    }
}

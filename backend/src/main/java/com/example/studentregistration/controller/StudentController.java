package com.example.studentregistration.controller;

import com.example.studentregistration.dto.StudentAttendanceEntryResponse;
import com.example.studentregistration.model.Student;
import com.example.studentregistration.repository.StudentRepository;
import com.example.studentregistration.service.AttendanceService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentController {
    private final StudentRepository studentRepository;
    private final AttendanceService attendanceService;

    public StudentController(StudentRepository studentRepository, AttendanceService attendanceService) {
        this.studentRepository = studentRepository;
        this.attendanceService = attendanceService;
    }

    @GetMapping("/me")
    public Student getMyStudentProfile(HttpSession session) {
        Object userId = session.getAttribute("appUserId");
        if (!(userId instanceof Number number)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in first");
        }
        return studentRepository.findByUserId(number.longValue())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No student profile is linked to this account"));
    }

    @GetMapping("/me/attendance")
    public List<StudentAttendanceEntryResponse> getMyAttendance(
            HttpSession session,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        Student student = getMyStudentProfile(session);
        LocalDate today = LocalDate.now();
        LocalDate fromDate = from == null ? today : from;
        LocalDate toDate = to == null ? fromDate : to;
        return attendanceService.getStudentAttendance(student.getStudentId(), fromDate, toDate);
    }

    @GetMapping("/{studentId}")
    public Student getStudent(@PathVariable Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
    }
}

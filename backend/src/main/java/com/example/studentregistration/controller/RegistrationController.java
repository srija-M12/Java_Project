package com.example.studentregistration.controller;

import com.example.studentregistration.dto.RegisterCoursesRequest;
import com.example.studentregistration.dto.RegistrationResponse;
import com.example.studentregistration.dto.RegistrationPolicyResponse;
import com.example.studentregistration.model.Student;
import com.example.studentregistration.repository.StudentRepository;
import com.example.studentregistration.service.RegistrationService;
import com.example.studentregistration.service.RegistrationDeadlineService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/registrations")
public class RegistrationController {
    private final RegistrationService registrationService;
    private final StudentRepository studentRepository;
    private final RegistrationDeadlineService registrationDeadlineService;

    public RegistrationController(RegistrationService registrationService, StudentRepository studentRepository,
                                  RegistrationDeadlineService registrationDeadlineService) {
        this.registrationService = registrationService;
        this.studentRepository = studentRepository;
        this.registrationDeadlineService = registrationDeadlineService;
    }

    @GetMapping("/policy")
    public RegistrationPolicyResponse getCurrentPolicy(HttpSession session) {
        Object userId = session.getAttribute("appUserId");
        if (!(userId instanceof Number number)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in first");
        }
        Student student = studentRepository.findByUserId(number.longValue())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "No student profile is linked to this account"));
        return registrationDeadlineService.getPolicy(student);
    }

    @PostMapping
    public RegistrationResponse register(@RequestBody RegisterCoursesRequest request, HttpSession session) {
        Long userId = ((Number) session.getAttribute("appUserId")).longValue();
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No student profile is linked to this account"));
        if (request == null || !student.getStudentId().equals(request.studentId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only register your own student account");
        }
        return registrationService.register(request);
    }

    @GetMapping("/{studentId}")
    public List<com.example.studentregistration.model.Course> getRegisteredCourses(@PathVariable Long studentId) {
        return registrationService.getCoursesForStudent(studentId);
    }
}

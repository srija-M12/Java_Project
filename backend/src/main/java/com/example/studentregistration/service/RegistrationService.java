package com.example.studentregistration.service;

import com.example.studentregistration.dto.RegisterCoursesRequest;
import com.example.studentregistration.dto.RegistrationResponse;
import com.example.studentregistration.model.Course;
import com.example.studentregistration.model.Registration;
import com.example.studentregistration.model.Student;
import com.example.studentregistration.repository.CourseRepository;
import com.example.studentregistration.repository.RegistrationRepository;
import com.example.studentregistration.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class RegistrationService {
    private static final int REQUIRED_COURSE_COUNT = 6;

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final RegistrationRepository registrationRepository;
    private final RegistrationDeadlineService registrationDeadlineService;

    public RegistrationService(
            StudentRepository studentRepository,
            CourseRepository courseRepository,
            RegistrationRepository registrationRepository,
            RegistrationDeadlineService registrationDeadlineService
    ) {
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.registrationRepository = registrationRepository;
        this.registrationDeadlineService = registrationDeadlineService;
    }

    @Transactional
    public RegistrationResponse register(RegisterCoursesRequest request) {
        if (request == null || request.studentId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "studentId is required");
        }
        if (request.courseIds() == null || request.courseIds().size() != REQUIRED_COURSE_COUNT
                || request.courseIds().stream().anyMatch(Objects::isNull)
                || new HashSet<>(request.courseIds()).size() != REQUIRED_COURSE_COUNT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select exactly six different courses");
        }

        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        if (!registrationDeadlineService.isOpen(
                student.getDepartment(), student.getAcademicYear(), student.getCurrentSemester())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The registration deadline for this semester has passed");
        }

        List<Course> courses = courseRepository.findAllById(request.courseIds());
        if (courses.size() != REQUIRED_COURSE_COUNT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more courses do not exist");
        }

        for (Course course : courses) {
            boolean sameDepartment = course.getDepartment().equalsIgnoreCase(student.getDepartment());
            boolean sameSemester = course.getSemester().equals(student.getCurrentSemester());
            boolean sameAcademicYear = course.getAcademicYear().equals(student.getAcademicYear());
            if (!sameDepartment || !sameSemester || !sameAcademicYear) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "All selected courses must match the student's department, semester, and academic year");
            }
        }

        List<Registration> existing = registrationRepository.findByStudentId(student.getStudentId());
        Set<Long> existingCourseIds = new HashSet<>();
        existing.forEach(registration -> existingCourseIds.add(registration.getCourseId()));
        if (!existingCourseIds.isEmpty()) {
            List<Course> existingCourses = courseRepository.findAllById(existingCourseIds);
            boolean alreadyRegisteredForTerm = existingCourses.stream().anyMatch(course ->
                    course.getDepartment().equalsIgnoreCase(student.getDepartment())
                            && course.getSemester().equals(student.getCurrentSemester())
                            && course.getAcademicYear().equals(student.getAcademicYear()));
            if (alreadyRegisteredForTerm) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Student already has a registration for this term");
            }
        }

        registrationRepository.saveAll(courses.stream()
                .map(course -> new Registration(student.getStudentId(), course.getCourseId()))
                .toList());

        return new RegistrationResponse(student.getStudentId(), request.courseIds(),
                "Student registered for six courses");
    }

    @Transactional(readOnly = true)
    public List<Course> getCoursesForStudent(Long studentId) {
        studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        List<Long> courseIds = registrationRepository.findByStudentId(studentId)
                .stream().map(Registration::getCourseId).toList();
        return courseRepository.findAllById(courseIds);
    }
}

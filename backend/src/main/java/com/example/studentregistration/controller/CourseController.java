package com.example.studentregistration.controller;

import com.example.studentregistration.model.Course;
import com.example.studentregistration.repository.CourseRepository;
import com.example.studentregistration.service.CourseDetailsService;
import com.example.studentregistration.dto.CourseDetailsResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {
    private final CourseRepository courseRepository;
    private final CourseDetailsService courseDetailsService;

    public CourseController(CourseRepository courseRepository, CourseDetailsService courseDetailsService) {
        this.courseRepository = courseRepository;
        this.courseDetailsService = courseDetailsService;
    }

    @GetMapping
    public List<Course> getCourses(
            @RequestParam String department,
            @RequestParam Byte semester,
            @RequestParam String academicYear
    ) {
        return courseRepository.findByDepartmentAndSemesterAndAcademicYear(
                department, semester, academicYear
        );
    }

    @GetMapping("/{courseId}/details")
    public CourseDetailsResponse getCourseDetails(@PathVariable Long courseId) {
        return courseDetailsService.getDetails(courseId);
    }
}

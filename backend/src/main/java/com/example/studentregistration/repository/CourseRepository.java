package com.example.studentregistration.repository;

import com.example.studentregistration.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByDepartmentAndSemesterAndAcademicYear(
            String department,
            Byte semester,
            String academicYear
    );
}
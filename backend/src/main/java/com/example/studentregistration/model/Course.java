package com.example.studentregistration.model;

import jakarta.persistence.*;

@Entity
@Table(name = "courses")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "course_code")
    private String courseCode;

    @Column(name = "course_name")
    private String courseName;

    @Column(name = "faculty_id")
    private Long facultyId;

    private String department;
    private Byte semester;

    @Column(name = "academic_year")
    private String academicYear;

    private Byte credits;

    @Column(name = "periods_per_week")
    private Byte periodsPerWeek;

    @Column(name = "course_type")
    private String courseType;

    public Course() {}

    public Long getCourseId() { return courseId; }
    public String getCourseCode() { return courseCode; }
    public String getCourseName() { return courseName; }
    public Long getFacultyId() { return facultyId; }
    public String getDepartment() { return department; }
    public Byte getSemester() { return semester; }
    public String getAcademicYear() { return academicYear; }
    public Byte getCredits() { return credits; }
    public Byte getPeriodsPerWeek() { return periodsPerWeek; }
    public String getCourseType() { return courseType; }
}

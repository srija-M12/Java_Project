package com.example.studentregistration.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "students")
public class Student {
    @Id
    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "register_number")
    private String registerNumber;

    @Column(name = "student_name")
    private String studentName;

    private String department;

    @Column(name = "academic_year")
    private String academicYear;

    @Column(name = "current_semester")
    private Byte currentSemester;

    public Student() {}

    public Long getStudentId() { return studentId; }
    public Long getUserId() { return userId; }
    public String getRegisterNumber() { return registerNumber; }
    public String getStudentName() { return studentName; }
    public String getDepartment() { return department; }
    public String getAcademicYear() { return academicYear; }
    public Byte getCurrentSemester() { return currentSemester; }
}
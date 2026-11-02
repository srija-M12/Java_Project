package com.example.studentregistration.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "faculty")
public class Faculty {
    @Id
    @Column(name = "faculty_id")
    private Long facultyId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "faculty_name")
    private String facultyName;

    private String department;

    public Faculty() {}

    public Long getFacultyId() { return facultyId; }
    public Long getUserId() { return userId; }
    public String getFacultyName() { return facultyName; }
    public String getDepartment() { return department; }
}
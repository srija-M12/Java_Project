package com.example.studentregistration.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "registrations")
public class Registration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "registration_id")
    private Long registrationId;

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "registered_at", insertable = false, updatable = false)
    private java.time.LocalDateTime registeredAt;

    protected Registration() {}

    public Registration(Long studentId, Long courseId) {
        this.studentId = studentId;
        this.courseId = courseId;
    }

    public Long getRegistrationId() { return registrationId; }
    public Long getStudentId() { return studentId; }
    public Long getCourseId() { return courseId; }
    public java.time.LocalDateTime getRegisteredAt() { return registeredAt; }
}
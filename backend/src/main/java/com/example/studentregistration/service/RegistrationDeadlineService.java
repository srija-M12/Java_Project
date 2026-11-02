package com.example.studentregistration.service;

import com.example.studentregistration.dto.RegistrationPolicyResponse;
import com.example.studentregistration.model.Student;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RegistrationDeadlineService {
    private static final int REQUIRED_COURSE_COUNT = 6;
    private final JdbcTemplate jdbcTemplate;

    public RegistrationDeadlineService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public RegistrationPolicyResponse getPolicy(Student student) {
        List<PolicyDetails> policies = jdbcTemplate.query(
                "SELECT policy_id, deadline_at, deadline_at > CURRENT_TIMESTAMP AS registration_open "
                        + "FROM registration_policies WHERE department = ? AND academic_year = ? AND semester = ?",
                (rs, rowNum) -> new PolicyDetails(rs.getLong("policy_id"),
                        rs.getTimestamp("deadline_at").toLocalDateTime(), rs.getBoolean("registration_open")),
                student.getDepartment(), student.getAcademicYear(), student.getCurrentSemester());
        if (policies.isEmpty()) {
            return new RegistrationPolicyResponse(student.getDepartment(), student.getAcademicYear(),
                    student.getCurrentSemester(), null, true, List.of());
        }

        PolicyDetails policy = policies.getFirst();
        List<RegistrationPolicyResponse.DefaultCourse> defaults = jdbcTemplate.query(
                "SELECT c.course_code, c.course_name FROM registration_default_courses d "
                        + "JOIN courses c ON c.course_id = d.course_id WHERE d.policy_id = ? ORDER BY c.course_id",
                (rs, rowNum) -> new RegistrationPolicyResponse.DefaultCourse(
                        rs.getString("course_code"), rs.getString("course_name")), policy.policyId());
        return new RegistrationPolicyResponse(student.getDepartment(), student.getAcademicYear(),
                student.getCurrentSemester(), policy.deadlineAt(), policy.open(), defaults);
    }

    @Scheduled(fixedDelayString = "${app.registration-policy-check-ms:60000}")
    @Transactional
    public void applyOverdueDefaults() {
        List<TermPolicy> policies = jdbcTemplate.query(
                "SELECT policy_id, department, academic_year, semester FROM registration_policies "
                        + "WHERE deadline_at <= CURRENT_TIMESTAMP AND processed_at IS NULL",
                (rs, rowNum) -> new TermPolicy(
                        rs.getLong("policy_id"), rs.getString("department"),
                        rs.getString("academic_year"), rs.getInt("semester"))
        );

        for (TermPolicy policy : policies) {
            int defaults = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM registration_default_courses d "
                            + "JOIN courses c ON c.course_id = d.course_id "
                            + "WHERE d.policy_id = ? AND c.department = ? AND c.academic_year = ? AND c.semester = ?",
                    Integer.class, policy.policyId(), policy.department(), policy.academicYear(), policy.semester());
            if (defaults != REQUIRED_COURSE_COUNT) {
                throw new IllegalStateException("Registration policy " + policy.policyId()
                        + " must have exactly six valid default courses; found " + defaults);
            }

            List<Long> studentIds = jdbcTemplate.queryForList(
                    "SELECT student_id FROM students WHERE department = ? AND academic_year = ? "
                            + "AND current_semester = ? FOR UPDATE",
                    Long.class, policy.department(), policy.academicYear(), policy.semester());

            for (Long studentId : studentIds) {
                Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM registrations r JOIN courses c ON c.course_id = r.course_id "
                                + "WHERE r.student_id = ? AND c.department = ? AND c.academic_year = ? AND c.semester = ?",
                        Integer.class, studentId, policy.department(), policy.academicYear(), policy.semester());
                if (count != null && count < REQUIRED_COURSE_COUNT) {
                    jdbcTemplate.update(
                            "DELETE t FROM timetable_entries t JOIN courses c ON c.course_id = t.course_id "
                                    + "WHERE t.student_id = ? AND c.department = ? AND c.academic_year = ? AND c.semester = ?",
                            studentId, policy.department(), policy.academicYear(), policy.semester());
                    jdbcTemplate.update(
                            "DELETE r FROM registrations r JOIN courses c ON c.course_id = r.course_id "
                                    + "WHERE r.student_id = ? AND c.department = ? AND c.academic_year = ? AND c.semester = ?",
                            studentId, policy.department(), policy.academicYear(), policy.semester());
                    jdbcTemplate.update(
                            "INSERT INTO registrations (student_id, course_id) "
                                    + "SELECT ?, d.course_id FROM registration_default_courses d "
                                    + "WHERE d.policy_id = ? ORDER BY d.course_id",
                            studentId, policy.policyId());
                }
            }
            jdbcTemplate.update(
                    "UPDATE registration_policies SET processed_at = CURRENT_TIMESTAMP "
                            + "WHERE policy_id = ? AND processed_at IS NULL",
                    policy.policyId());
        }
    }

    public boolean isOpen(String department, String academicYear, int semester) {
        List<Boolean> open = jdbcTemplate.query(
                "SELECT deadline_at > CURRENT_TIMESTAMP FROM registration_policies "
                        + "WHERE department = ? AND academic_year = ? AND semester = ?",
                (rs, rowNum) -> rs.getBoolean(1), department, academicYear, semester);
        return open.isEmpty() || open.getFirst();
    }

    private record TermPolicy(Long policyId, String department, String academicYear, int semester) {}
    private record PolicyDetails(Long policyId, java.time.LocalDateTime deadlineAt, boolean open) {}
}

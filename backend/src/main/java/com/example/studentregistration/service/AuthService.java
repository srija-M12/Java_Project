package com.example.studentregistration.service;

import com.example.studentregistration.dto.LoginRequest;
import com.example.studentregistration.dto.LoginResponse;
import com.example.studentregistration.dto.StudentSignupRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Locale;
import java.util.List;

@Service
public class AuthService {
    private final JdbcTemplate jdbcTemplate;
    private final String campusCode;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(JdbcTemplate jdbcTemplate, @Value("${app.security.campus-code}") String campusCode) {
        this.jdbcTemplate = jdbcTemplate;
        this.campusCode = campusCode;
    }

    @Transactional
    public LoginResponse signupStudent(StudentSignupRequest request) {
        if (request == null || blank(request.username()) || blank(request.email()) || blank(request.password())
                || blank(request.campusCode())
                || blank(request.studentName()) || blank(request.registerNumber()) || blank(request.department())
                || blank(request.academicYear()) || request.currentSemester() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Complete all required sign-up fields");
        }
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String registerNumber = request.registerNumber().trim();
        String studentName = request.studentName().trim();
        String department = request.department().trim();
        String academicYear = request.academicYear().trim();
        requireCampusCode(request.campusCode());
        validateSignup(username, email, request.password(), studentName, registerNumber, department, academicYear,
                request.currentSemester());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO users (username, password_hash, role, email) VALUES (?, ?, 'STUDENT', ?)",
                        Statement.RETURN_GENERATED_KEYS);
                statement.setString(1, username);
                statement.setString(2, passwordEncoder.encode(request.password()));
                statement.setString(3, email);
                return statement;
            }, keyHolder);
            Number generatedId = keyHolder.getKey();
            if (generatedId == null) throw new IllegalStateException("Could not create user account");
            Long userId = generatedId.longValue();
            jdbcTemplate.update(
                    "INSERT INTO students (user_id, register_number, student_name, department, academic_year, current_semester) "
                            + "VALUES (?, ?, ?, ?, ?, ?)",
                    userId, registerNumber, studentName, department, academicYear, request.currentSemester());
            return new LoginResponse(userId, username, email, "STUDENT");
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "That username, email, or register number is already in use");
        }
    }

    private void validateSignup(String username, String email, String password, String studentName,
                                String registerNumber, String department, String academicYear, int semester) {
        if (!username.matches("[A-Za-z0-9._-]{3,50}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Username must be 3 to 50 characters using letters, numbers, dots, underscores, or hyphens");
        }
        if (email.length() > 254 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid email address");
        }
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 8
                || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be between 8 and 72 bytes");
        }
        if (studentName.length() > 120 || registerNumber.length() > 30 || department.length() > 80) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more profile fields are too long");
        }
        if (!academicYear.matches("\\d{4}-\\d{4}") || semester < 1 || semester > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter an academic year like 2026-2027 and a valid semester");
        }
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }

    private void requireCampusCode(String suppliedCode) {
        if (suppliedCode == null || !MessageDigest.isEqual(
                campusCode.getBytes(StandardCharsets.UTF_8), suppliedCode.trim().getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid campus code");
        }
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        if (request == null || request.username() == null || request.password() == null || request.campusCode() == null
                || request.username().isBlank() || request.password().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username, password, and campus code are required");
        }
        requireCampusCode(request.campusCode());
        List<Account> accounts = jdbcTemplate.query(
                "SELECT user_id, username, password_hash, role, email FROM users WHERE username = ?",
                (rs, rowNum) -> new Account(
                        rs.getLong("user_id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("role"),
                        rs.getString("email")
                ),
                request.username().trim()
        );
        if (accounts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        Account account = accounts.getFirst();
        boolean alreadyHashed = isBcryptHash(account.passwordHash());
        boolean valid = alreadyHashed
                ? passwordEncoder.matches(request.password(), account.passwordHash())
                : MessageDigest.isEqual(
                        request.password().getBytes(StandardCharsets.UTF_8),
                        account.passwordHash().getBytes(StandardCharsets.UTF_8)
                );
        if (!valid) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        if (!alreadyHashed) {
            jdbcTemplate.update(
                    "UPDATE users SET password_hash = ? WHERE user_id = ?",
                    passwordEncoder.encode(request.password()), account.userId()
            );
        }
        return new LoginResponse(account.userId(), account.username(), account.email(), account.role());
    }

    private boolean isBcryptHash(String value) {
        return value != null && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }

    private record Account(Long userId, String username, String passwordHash, String role, String email) {}
}

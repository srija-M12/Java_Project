package com.example.studentregistration.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SessionAuthorizationInterceptor implements HandlerInterceptor {
    private final JdbcTemplate jdbcTemplate;

    public SessionAuthorizationInterceptor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute("appUserId") instanceof Number userId)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Please log in first\"}");
            return false;
        }

        String path = request.getRequestURI();
        String role = String.valueOf(session.getAttribute("appUserRole"));
        String requiredRole = path.startsWith("/api/faculty/") ? "FACULTY"
                : path.startsWith("/api/students/") || path.startsWith("/api/registrations")
                || path.startsWith("/api/timetables/") ? "STUDENT" : null;
        if (requiredRole != null && !requiredRole.equals(role) && !"ADMIN".equals(role)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Your account does not have access to this feature\"}");
            return false;
        }

        if ("ADMIN".equals(role)) {
            return true;
        }
        Long id = userId.longValue();
        if (!isOwner(request, path, id, response)) {
            return false;
        }
        return true;
    }

    private boolean isOwner(HttpServletRequest request, String path, Long userId, HttpServletResponse response)
            throws Exception {
        String[] parts = path.split("/");
        if (path.startsWith("/api/students/") || path.startsWith("/api/registrations/")
                || path.startsWith("/api/timetables/")) {
            String idText = parts[parts.length - 1];
            if (!idText.matches("\\d+")) {
                return true;
            }
            return ownerMatches("SELECT user_id FROM students WHERE student_id = ?", Long.parseLong(idText), userId, response);
        }
        if (path.startsWith("/api/faculty/availability/")) {
            String availabilityId = parts[parts.length - 1];
            return ownerMatches(
                    "SELECT f.user_id FROM faculty f JOIN faculty_availability a ON a.faculty_id = f.faculty_id "
                            + "WHERE a.availability_id = ?",
                    Long.parseLong(availabilityId), userId, response
            );
        }
        if (path.startsWith("/api/faculty/")) {
            String facultyId = parts[3];
            if (facultyId.matches("\\d+")) {
                return ownerMatches("SELECT user_id FROM faculty WHERE faculty_id = ?", Long.parseLong(facultyId), userId, response);
            }
        }
        return true;
    }

    private boolean ownerMatches(String sql, Long id, Long userId, HttpServletResponse response) throws Exception {
        Long ownerId;
        try {
            ownerId = jdbcTemplate.queryForObject(sql, Long.class, id);
        } catch (org.springframework.dao.EmptyResultDataAccessException exception) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return false;
        }
        if (!userId.equals(ownerId)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"You can only access your own records\"}");
            return false;
        }
        return true;
    }
}
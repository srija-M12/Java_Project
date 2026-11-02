package com.example.studentregistration.controller;

import com.example.studentregistration.dto.LoginRequest;
import com.example.studentregistration.dto.LoginResponse;
import com.example.studentregistration.dto.StudentSignupRequest;
import com.example.studentregistration.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        LoginResponse response = authService.login(request);
        HttpSession session = servletRequest.getSession(true);
        session.setAttribute("appUserId", response.userId());
        session.setAttribute("appUserRole", response.role());
        return response;
    }

    @PostMapping("/signup")
    public LoginResponse signup(@RequestBody StudentSignupRequest request, HttpServletRequest servletRequest) {
        LoginResponse response = authService.signupStudent(request);
        HttpSession session = servletRequest.getSession(true);
        session.setAttribute("appUserId", response.userId());
        session.setAttribute("appUserRole", response.role());
        return response;
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest servletRequest) {
        HttpSession session = servletRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}

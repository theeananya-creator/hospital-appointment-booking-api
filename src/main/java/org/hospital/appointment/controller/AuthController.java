package org.hospital.appointment.controller;

import jakarta.validation.Valid;
import org.hospital.appointment.dto.AuthRequests;
import org.hospital.appointment.service.AuthService;
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

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthRequests.TokenResponse register(@Valid @RequestBody AuthRequests.Register request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthRequests.TokenResponse login(@Valid @RequestBody AuthRequests.Login request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthRequests.TokenResponse refresh(@Valid @RequestBody AuthRequests.Refresh request) {
        return authService.refresh(request.refreshToken());
    }
}

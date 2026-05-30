package com.sistercontrol.server.controller;

import com.sistercontrol.server.dto.ApiDtos.AuthRequest;
import com.sistercontrol.server.dto.ApiDtos.AuthResponse;
import com.sistercontrol.server.dto.ApiDtos.LoginRequest;
import com.sistercontrol.server.dto.ApiDtos.OkResponse;
import com.sistercontrol.server.security.CurrentUser;
import com.sistercontrol.server.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService authService;
    private final CurrentUser currentUser;

    public AuthController(AuthService authService, CurrentUser currentUser) {
        this.authService = authService;
        this.currentUser = currentUser;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody AuthRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @DeleteMapping("/me")
    public OkResponse deleteMe() {
        authService.deleteAccount(currentUser.get());
        return OkResponse.success();
    }
}

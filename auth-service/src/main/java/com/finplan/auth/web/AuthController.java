package com.finplan.auth.web;

import com.finplan.auth.service.AuthService;
import com.finplan.auth.web.AuthDtos.AuthResponse;
import com.finplan.auth.web.AuthDtos.LoginRequest;
import com.finplan.auth.web.AuthDtos.RegisterRequest;
import com.finplan.auth.web.AuthDtos.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) { this.service = service; }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest req) {
        return service.register(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return service.login(req);
    }

    @GetMapping("/me")
    public UserResponse me(Authentication auth) {
        return service.me(UUID.fromString(auth.getName()));
    }
}

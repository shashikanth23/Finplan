package com.finplan.auth.service;

import com.finplan.auth.exception.ApiException;
import com.finplan.auth.security.JwtService;
import com.finplan.auth.user.Role;
import com.finplan.auth.user.User;
import com.finplan.auth.user.UserRepository;
import com.finplan.auth.web.AuthDtos.AuthResponse;
import com.finplan.auth.web.AuthDtos.LoginRequest;
import com.finplan.auth.web.AuthDtos.RegisterRequest;
import com.finplan.auth.web.AuthDtos.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public UserResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user = users.save(new User(email, encoder.encode(req.password()), req.fullName().trim(), Role.CUSTOMER));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        // Same error for unknown email and wrong password, so we don't leak which emails exist.
        User user = users.findByEmailIgnoreCase(req.email().trim())
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return new AuthResponse(jwt.generateAccessToken(user), "Bearer", jwt.accessTtlSeconds());
    }

    @Transactional(readOnly = true)
    public UserResponse me(UUID userId) {
        return users.findById(userId).map(this::toResponse)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(u.getId().toString(), u.getEmail(), u.getFullName(), u.getRole().name());
    }
}

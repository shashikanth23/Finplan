package com.finplan.auth;

import com.finplan.auth.exception.ApiException;
import com.finplan.auth.security.JwtService;
import com.finplan.auth.service.AuthService;
import com.finplan.auth.user.Role;
import com.finplan.auth.user.User;
import com.finplan.auth.user.UserRepository;
import com.finplan.auth.web.AuthDtos.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    @Test
    void loginUsesSameUnauthorizedErrorForUnknownEmailAndWrongPassword() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        JwtService jwt = new JwtService("unit-test-secret-that-is-at-least-32-bytes-long", 15);
        when(users.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCase("known@example.com"))
                .thenReturn(Optional.of(new User("known@example.com", "stored-hash", "Known User", Role.CUSTOMER)));
        when(encoder.matches("wrong-password", "stored-hash")).thenReturn(false);
        AuthService service = new AuthService(users, encoder, jwt);

        ApiException unknownEmail = assertThrows(ApiException.class,
                () -> service.login(new LoginRequest("missing@example.com", "wrong-password")));
        ApiException wrongPassword = assertThrows(ApiException.class,
                () -> service.login(new LoginRequest("known@example.com", "wrong-password")));

        assertEquals(unknownEmail.getStatus(), wrongPassword.getStatus());
        assertEquals(unknownEmail.getMessage(), wrongPassword.getMessage());
        assertEquals("Invalid email or password", unknownEmail.getMessage());
    }
}

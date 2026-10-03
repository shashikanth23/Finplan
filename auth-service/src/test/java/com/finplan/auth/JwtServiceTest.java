package com.finplan.auth;

import com.finplan.auth.security.JwtService;
import com.finplan.auth.user.Role;
import com.finplan.auth.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-that-is-at-least-32-bytes-long";

    @Test
    void tokenRoundTripsSubjectAndRole() {
        JwtService jwt = new JwtService(SECRET, 15);
        User user = new User("a@b.com", "hash", "A B", Role.CUSTOMER);

        Claims claims = jwt.parse(jwt.generateAccessToken(user));

        assertEquals(user.getId().toString(), claims.getSubject());
        assertEquals("CUSTOMER", claims.get("role", String.class));
    }

    @Test
    void tokenSignedWithDifferentSecretIsRejected() {
        User user = new User("a@b.com", "hash", "A B", Role.CUSTOMER);
        String token = new JwtService(SECRET, 15).generateAccessToken(user);
        JwtService other = new JwtService("another-secret-that-is-also-32-bytes-long!!", 15);

        assertThrows(JwtException.class, () -> other.parse(token));
    }
}

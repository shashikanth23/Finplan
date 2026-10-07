package com.finplan.security;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokensTest {
    @Test
    void rs256TokenCanBeVerifiedWithPublicKeyOnly() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var pair = generator.generateKeyPair();
        String privateKey = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
        String publicKey = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());

        String token = JwtTokens.sign("user-1", Map.of("role", "CUSTOMER"), Instant.now(),
                Instant.now().plusSeconds(60), "RS256", "", privateKey);

        assertEquals("user-1", JwtTokens.verify(token, "RS256", "", publicKey).getSubject());
        assertThrows(RuntimeException.class, () -> JwtTokens.verify(token, "RS256", "", privateKey));
    }

    @Test
    void productionRejectsSharedSecretMode() {
        assertThrows(IllegalStateException.class, () -> JwtTokens.requireProductionMode("HS256", "prod"));
        assertDoesNotThrow(() -> JwtTokens.requireProductionMode("RS256", "prod"));
        assertDoesNotThrow(() -> JwtTokens.requireProductionMode("HS256", "local"));
    }
}

package com.finplan.auth.security;

import com.finplan.auth.user.User;
import com.finplan.security.JwtTokens;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Service
public class JwtService {

    private final String mode;
    private final String secret;
    private final String privateKey;
    private final String publicKey;
    private final Duration accessTtl;

    @Autowired
    public JwtService(@Value("${finplan.jwt.mode:HS256}") String mode,
                      @Value("${finplan.jwt.secret:}") String secret,
                      @Value("${finplan.jwt.private-key:}") String privateKey,
                      @Value("${finplan.jwt.public-key:}") String publicKey,
                      @Value("${spring.profiles.active:}") String activeProfiles,
                      @Value("${finplan.jwt.access-token-minutes}") long accessMinutes) {
        JwtTokens.validateSigningConfiguration(mode, secret, privateKey, activeProfiles);
        if ("RS256".equalsIgnoreCase(mode)) {
            JwtTokens.validateVerificationConfiguration(mode, secret, publicKey, activeProfiles);
            JwtTokens.validateKeyPair(mode, privateKey, publicKey);
        }
        this.mode = mode;
        this.secret = secret;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.accessTtl = Duration.ofMinutes(accessMinutes);
    }

    public JwtService(String secret, long accessMinutes) {
        this("HS256", secret, "", "", "", accessMinutes);
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return JwtTokens.sign(user.getId().toString(),
                Map.of("email", user.getEmail(), "role", user.getRole().name()),
                now, now.plus(accessTtl), mode, secret, privateKey);
    }

    public Claims parse(String token) {
        return JwtTokens.verify(token, mode, secret, publicKey);
    }

    public long accessTtlSeconds() {
        return accessTtl.toSeconds();
    }
}

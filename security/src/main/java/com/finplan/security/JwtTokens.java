package com.finplan.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

public final class JwtTokens {
    private JwtTokens() {}

    public static String sign(String subject, Map<String, ?> claims, Instant issuedAt, Instant expiresAt,
                              String mode, String secret, String privateKey) {
        var builder = Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt));
        if ("RS256".equalsIgnoreCase(mode)) {
            return builder.signWith(readPrivateKey(privateKey), Jwts.SIG.RS256).compact();
        }
        if ("HS256".equalsIgnoreCase(mode)) {
            return builder.signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256).compact();
        }
        throw new IllegalArgumentException("Unsupported JWT mode: " + mode);
    }

    public static Claims verify(String token, String mode, String secret, String publicKey) {
        if ("RS256".equalsIgnoreCase(mode)) {
            return Jwts.parser().verifyWith(readPublicKey(publicKey)).build().parseSignedClaims(token).getPayload();
        }
        if ("HS256".equalsIgnoreCase(mode)) {
            return Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                    .build().parseSignedClaims(token).getPayload();
        }
        throw new IllegalArgumentException("Unsupported JWT mode: " + mode);
    }

    public static void requireProductionMode(String mode, String activeProfiles) {
        boolean production = activeProfiles != null && java.util.Arrays.stream(activeProfiles.split(","))
                .map(String::trim).anyMatch(profile -> profile.equals("prod"));
        if (production && !"RS256".equalsIgnoreCase(mode)) {
            throw new IllegalStateException("Production requires JWT_MODE=RS256");
        }
    }

    public static void validateSigningConfiguration(String mode, String secret, String privateKey,
                                                    String activeProfiles) {
        requireProductionMode(mode, activeProfiles);
        if ("RS256".equalsIgnoreCase(mode)) readPrivateKey(privateKey);
        else if ("HS256".equalsIgnoreCase(mode)) Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        else throw new IllegalArgumentException("Unsupported JWT mode: " + mode);
    }

    public static void validateVerificationConfiguration(String mode, String secret, String publicKey,
                                                         String activeProfiles) {
        requireProductionMode(mode, activeProfiles);
        if ("RS256".equalsIgnoreCase(mode)) readPublicKey(publicKey);
        else if ("HS256".equalsIgnoreCase(mode)) Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        else throw new IllegalArgumentException("Unsupported JWT mode: " + mode);
    }

    public static void validateKeyPair(String mode, String privateKey, String publicKey) {
        if (!"RS256".equalsIgnoreCase(mode)) return;
        RSAPrivateKey signingKey = (RSAPrivateKey) readPrivateKey(privateKey);
        RSAPublicKey verificationKey = (RSAPublicKey) readPublicKey(publicKey);
        if (!signingKey.getModulus().equals(verificationKey.getModulus())) {
            throw new IllegalStateException("JWT_PRIVATE_KEY and JWT_PUBLIC_KEY do not form a key pair");
        }
    }

    private static PrivateKey readPrivateKey(String encoded) {
        try {
            byte[] der = Base64.getDecoder().decode(requireKey(encoded, "JWT_PRIVATE_KEY"));
            PrivateKey key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
            if (!(key instanceof RSAPrivateKey rsa) || rsa.getModulus().bitLength() < 2048) {
                throw new IllegalArgumentException("RSA key must be at least 2048 bits");
            }
            return key;
        } catch (Exception e) {
            throw new IllegalStateException("JWT_PRIVATE_KEY must be base64-encoded PKCS#8 RSA key data", e);
        }
    }

    private static PublicKey readPublicKey(String encoded) {
        try {
            byte[] der = Base64.getDecoder().decode(requireKey(encoded, "JWT_PUBLIC_KEY"));
            PublicKey key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
            if (!(key instanceof RSAPublicKey rsa) || rsa.getModulus().bitLength() < 2048) {
                throw new IllegalArgumentException("RSA key must be at least 2048 bits");
            }
            return key;
        } catch (Exception e) {
            throw new IllegalStateException("JWT_PUBLIC_KEY must be base64-encoded X.509 RSA key data", e);
        }
    }

    private static String requireKey(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " is required for RS256");
        return value;
    }
}

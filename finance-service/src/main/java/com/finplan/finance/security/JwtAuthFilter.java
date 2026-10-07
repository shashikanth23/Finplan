package com.finplan.finance.security;

import io.jsonwebtoken.JwtException;
import com.finplan.security.JwtTokens;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final String mode;
    private final String secret;
    private final String publicKey;

    public JwtAuthFilter(String mode, String secret, String publicKey, String activeProfiles) {
        JwtTokens.validateVerificationConfiguration(mode, secret, publicKey, activeProfiles);
        this.mode = mode;
        this.secret = secret;
        this.publicKey = publicKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                var c = JwtTokens.verify(header.substring(7), mode, secret, publicKey);
                var auth = new UsernamePasswordAuthenticationToken(c.getSubject(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + c.get("role", String.class))));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException | IllegalArgumentException ignored) {
                // invalid or expired token: request stays unauthenticated -> 401
            }
        }
        chain.doFilter(req, res);
    }
}

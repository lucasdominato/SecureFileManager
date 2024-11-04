package com.lucasdominato.securefilemanager.security;

import com.lucasdominato.securefilemanager.exception.InvalidJwtAuthenticationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    private final SecretKey secretKey;

    private static final long JWT_TOKEN_VALIDITY = 1800000; // 30 minutes

    public JwtUtil(@Value("${jwt.secret-key}") final String base64EncodedSecretKey) {
        this.secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64EncodedSecretKey));
    }

    public String generateToken(final String username,
                                final String name,
                                final String email,
                                final String dateOfBirth) {
        final Date issuedAt = new Date(System.currentTimeMillis());
        final Date expiration = new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY);

        return generateToken(username, name, email, dateOfBirth, issuedAt, expiration);
    }

    public String generateToken(final String username,
                                final String name,
                                final String email,
                                final String dateOfBirth,
                                final Date issuedAt,
                                final Date expiration) {
        return generateToken(username, name, email, dateOfBirth, issuedAt, expiration, secretKey);
    }

    public String generateToken(final String username,
                                final String name,
                                final String email,
                                final String dateOfBirth,
                                final Date issuedAt,
                                final Date expiration,
                                final SecretKey secretKey) {
        return Jwts.builder()
                .subject(username)
                .claim("name", name)
                .claim("email", email)
                .claim("dateOfBirth", dateOfBirth)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    public Claims getValidClaimsFromToken(final String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("Token expired: {}", e.getMessage());
            throw new InvalidJwtAuthenticationException("Token has expired", e);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid token: {}", e.getMessage());
            throw new InvalidJwtAuthenticationException("Invalid JWT token", e);
        }
    }
}
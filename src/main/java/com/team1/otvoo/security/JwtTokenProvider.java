package com.team1.otvoo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private SecretKey secretKey;

    private static final long ACCESS_TOKEN_EXPIRATION_MS = 1000 * 60 * 60;
    private static final long REFRESH_TOKEN_EXPIRATION_MS = 1000 * 60 * 60 * 24;

    @Value("${jwt.secret:websocket-chat-secret-key-256-bit-minimum-length-required}")
    private String secret;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(String username) {
        return createToken(username, ACCESS_TOKEN_EXPIRATION_MS);
    }

    public String createRefreshToken(String username) {
        return createToken(username, REFRESH_TOKEN_EXPIRATION_MS);
    }

    private String createToken(String username, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
            .subject(username)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact();
    }

    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();

        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.warn("❌ JWT 토큰 유효성 검사 실패: {}", e.getMessage());
            return false;
        }
    }
}

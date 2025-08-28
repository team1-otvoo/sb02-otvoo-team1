package com.team1.otvoo.security;

import com.team1.otvoo.auth.token.RedisRefreshTokenStore;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtTokenProvider {

    private SecretKey secretKey;

    private static final long ACCESS_TOKEN_EXPIRATION_MS = 1000 * 60 * 60; // 1시간
    private static final long REFRESH_TOKEN_EXPIRATION_MS = 1000 * 60 * 60 * 24; // 24시간

    @Value("${jwt.secret:websocket-chat-secret-key-256-bit-minimum-length-required}")
    private String secret;

    private final RedisRefreshTokenStore refreshTokenStore;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        log.info("🔑 JWT Secret Key 초기화 완료");
    }

    public String createAccessToken(UUID userId, String role, String email) {
        Instant now = Instant.now();
        String accessToken = Jwts.builder()
            .setSubject(email)
            .claim("userId", userId.toString())
            .claim("role", role)
            .claim("email", email)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plusMillis(ACCESS_TOKEN_EXPIRATION_MS)))
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact();

        log.debug("🛠 액세스 토큰 생성: userId={}, email={}, 만료시간={}", userId, email, ACCESS_TOKEN_EXPIRATION_MS);
        return accessToken;
    }

    public String createRefreshToken(UUID userId, String role, String email) {
        Instant now = Instant.now();
        String refreshToken = Jwts.builder()
            .setSubject(email)
            .claim("userId", userId.toString())
            .claim("role", role)
            .claim("email", email)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plusMillis(REFRESH_TOKEN_EXPIRATION_MS)))
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact();

        refreshTokenStore.save(userId, refreshToken);
        log.debug("💾 RefreshToken 저장: userId={}, email={}, 토큰 만료시간={}", userId, email, REFRESH_TOKEN_EXPIRATION_MS);
        return refreshToken;
    }

    public UUID getUserIdFromToken(String token) {
        try {
            String userIdString = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("userId", String.class);

            return UUID.fromString(userIdString);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("⚠️ JWT 토큰에서 사용자 아이디 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    public String getEmailFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

            return claims.get("email", String.class);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("⚠️ JWT 토큰에서 이메일 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token);

            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("⚠️ JWT 토큰 유효성 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    public long getExpirationSecondsLeft(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

            Date expirationDate = claims.getExpiration();
            long now = System.currentTimeMillis();
            if (expirationDate == null) return 0;

            long diffMillis = expirationDate.getTime() - now;
            return diffMillis > 0 ? diffMillis / 1000 : 0;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("⚠️ JWT 토큰 남은 만료시간 추출 실패: {}", e.getMessage());
            return 0;
        }
    }

    public int getRefreshTokenValidityInSeconds() {
        return (int) (REFRESH_TOKEN_EXPIRATION_MS / 1000);
    }
}
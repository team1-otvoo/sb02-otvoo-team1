package com.team1.otvoo.security;

import com.team1.otvoo.auth.token.RedisAccessTokenStore;
import com.team1.otvoo.auth.token.RedisRefreshTokenStore;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
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

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtTokenProvider {

    private SecretKey secretKey;

    private static final long ACCESS_TOKEN_EXPIRATION_MS = 1000 * 60 * 60;           // 1시간
    private static final long REFRESH_TOKEN_EXPIRATION_MS = 1000 * 60 * 60 * 24;     // 24시간

    @Value("${jwt.secret:websocket-chat-secret-key-256-bit-minimum-length-required}")
    private String secret;

    private final RedisAccessTokenStore accessTokenStore;
    private final RedisRefreshTokenStore refreshTokenStore;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        log.info("🔑 JWT Secret Key 초기화 완료");
    }

    public String createAccessToken(String userId) {
        Instant now = Instant.now();
        String accessToken = Jwts.builder()
            .setSubject(userId)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plusMillis(ACCESS_TOKEN_EXPIRATION_MS)))
            .signWith(secretKey)
            .compact();

        log.debug("🛠 액세스 토큰 생성: userId={}, 만료시간={}", userId, ACCESS_TOKEN_EXPIRATION_MS);
        return accessToken;
    }

    public String createRefreshToken(String userId) {
        Instant now = Instant.now();
        String refreshToken = Jwts.builder()
            .setSubject(userId)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plusMillis(REFRESH_TOKEN_EXPIRATION_MS)))
            .signWith(secretKey)
            .compact();

        refreshTokenStore.save(userId, refreshToken);
        log.debug("💾 RefreshToken 저장: userId={}, 토큰 만료시간={}", userId, REFRESH_TOKEN_EXPIRATION_MS);
        return refreshToken;
    }

    public boolean validateToken(String token) {
        try {
            Jws<Claims> claimsJws = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token);

            boolean isBlacklisted = accessTokenStore.isBlacklisted(token);
            if (isBlacklisted) {
                log.warn("🚫 블랙리스트에 등록된 토큰 사용 시도");
                return false;
            }

            return !claimsJws.getBody().getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("⚠️ JWT 토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    public String getUserIdFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
        } catch (JwtException e) {
            log.warn("⚠️ JWT 토큰에서 사용자 아이디 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    public long getExpiration(String token) {
        try {
            Date expiration = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();

            long now = System.currentTimeMillis();
            return (expiration.getTime() - now) / 1000;
        } catch (JwtException e) {
            log.warn("⚠️ JWT 토큰 만료시간 조회 실패: {}", e.getMessage());
            return 0;
        }
    }
}
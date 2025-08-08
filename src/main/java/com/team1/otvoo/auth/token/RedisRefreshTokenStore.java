package com.team1.otvoo.auth.token;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

  private final StringRedisTemplate redisTemplate;

  @Value("${jwt.refresh-token-expiration-ms:86400000}") // 1일 (기본값)
  private long refreshTokenExpirationMs;

  private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

  private String getRefreshTokenKey(UUID userId) {
    return REFRESH_TOKEN_PREFIX + userId;
  }

  @Override
  public void save(UUID userId, String refreshToken) {
    redisTemplate.opsForValue().set(getRefreshTokenKey(userId), refreshToken, Duration.ofMillis(refreshTokenExpirationMs));
  }

  @Override
  public String get(UUID userId) {
    return redisTemplate.opsForValue().get(getRefreshTokenKey(userId));
  }

  @Override
  public void remove(UUID userId) {
    redisTemplate.delete(getRefreshTokenKey(userId));
  }
}
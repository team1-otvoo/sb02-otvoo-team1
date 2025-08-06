package com.team1.otvoo.auth.token;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

  private final StringRedisTemplate redisTemplate;

  @Value("${jwt.refresh-token-expiration-ms:86400000}") // 1일 (기본값)
  private long refreshTokenExpirationMs;

  private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
  private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

  private String getRefreshTokenKey(String userId) {
    return REFRESH_TOKEN_PREFIX + userId;
  }

  @Override
  public void save(String userId, String refreshToken) {
    redisTemplate.opsForValue().set(getRefreshTokenKey(userId), refreshToken, Duration.ofMillis(refreshTokenExpirationMs));
  }

  @Override
  public String get(String userId) {
    return redisTemplate.opsForValue().get(getRefreshTokenKey(userId));
  }

  @Override
  public void remove(String userId) {
    redisTemplate.delete(getRefreshTokenKey(userId));
  }

  @Override
  public void blacklistAccessToken(String accessToken, long expirationSeconds) {
    redisTemplate.opsForValue().set(BLACKLIST_PREFIX + accessToken, "1", Duration.ofSeconds(expirationSeconds));
  }

  @Override
  public boolean isBlacklisted(String accessToken) {
    return redisTemplate.hasKey(BLACKLIST_PREFIX + accessToken);
  }
}
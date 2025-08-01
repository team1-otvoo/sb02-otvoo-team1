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

  private String getKey(String userId) {
    return "refresh_token:" + userId;
  }

  @Override
  public void save(String userId, String refreshToken) {
    redisTemplate.opsForValue().set(getKey(userId), refreshToken, Duration.ofMillis(refreshTokenExpirationMs));
  }

  @Override
  public String get(String userId) {
    return redisTemplate.opsForValue().get(getKey(userId));
  }

  @Override
  public void remove(String userId) {
    redisTemplate.delete(getKey(userId));
  }
}

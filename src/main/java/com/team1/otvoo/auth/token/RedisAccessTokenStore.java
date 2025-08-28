package com.team1.otvoo.auth.token;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RedisAccessTokenStore implements AccessTokenStore {

  private final StringRedisTemplate redisTemplate;

  private static final String ACCESS_TOKEN_PREFIX = "access_token:";
  private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

  private String getAccessTokenKey(UUID userId) {
    return ACCESS_TOKEN_PREFIX + userId;
  }

  @Override
  public void save(UUID userId, String accessToken, long expirationSeconds) {
    redisTemplate.opsForValue().set(getAccessTokenKey(userId), accessToken, Duration.ofSeconds(expirationSeconds));
  }

  @Override
  public String get(UUID userId) {
    return redisTemplate.opsForValue().get(getAccessTokenKey(userId));
  }

  @Override
  public void remove(UUID userId) {
    redisTemplate.delete(getAccessTokenKey(userId));
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
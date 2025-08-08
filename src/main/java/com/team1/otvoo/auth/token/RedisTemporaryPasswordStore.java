package com.team1.otvoo.auth.token;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisTemporaryPasswordStore implements TemporaryPasswordStore {

  private final StringRedisTemplate redisTemplate;
  private static final String TEMP_PASSWORD_PREFIX = "temp-pwd:";
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void save(UUID userId, TemporaryPassword tempPassword, Duration duration) {
    long expirationTime = System.currentTimeMillis() + duration.toMillis();
    TemporaryPassword temporaryPassword = new TemporaryPassword(tempPassword.getPassword(), expirationTime);

    try {
      String jsonPassword = objectMapper.writeValueAsString(temporaryPassword);
      redisTemplate.opsForValue().set(TEMP_PASSWORD_PREFIX + userId.toString(), jsonPassword);
      log.debug("임시 비밀번호 저장 : userId={}, 만료시간={}", userId, expirationTime);
    } catch (Exception e) {
      throw new RuntimeException("임시 비밀번호 객체를 Redis에 저장하는 데 실패했습니다.", e);
    }
  }

  @Override
  public TemporaryPassword get(UUID userId) {
    String passwordData = redisTemplate.opsForValue().get(TEMP_PASSWORD_PREFIX + userId.toString());
    if (passwordData != null) {
      try {
        return objectMapper.readValue(passwordData, TemporaryPassword.class);
      } catch (IOException e) {
        throw new RuntimeException("임시 비밀번호 객체를 역직렬화하는 데 실패했습니다.", e);
      }
    }
    return null;
  }

  @Override
  public void remove(UUID userId) {
    redisTemplate.delete(TEMP_PASSWORD_PREFIX + userId.toString());
    log.debug("임시 비밀번호 삭제: userId={}", userId);
  }
}
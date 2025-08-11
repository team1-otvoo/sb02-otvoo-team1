package com.team1.otvoo.auth.token;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class RedisRefreshTokenStoreTest {

  @Mock
  private StringRedisTemplate redisTemplate;

  @Mock
  private ValueOperations<String, String> valueOps;

  @InjectMocks
  private RedisRefreshTokenStore refreshTokenStore;

  private UUID userId;
  private String refreshToken;

  @BeforeEach
  void setup() {
    userId = UUID.randomUUID();
    refreshToken = "testRefreshToken";
    lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);

    org.springframework.test.util.ReflectionTestUtils.setField(refreshTokenStore, "refreshTokenExpirationMs", 86400000L);
  }

  @Test
  @DisplayName("RefreshToken 저장 테스트")
  void saveTest() {
    refreshTokenStore.save(userId, refreshToken);

    verify(valueOps).set("refresh_token:" + userId, refreshToken, Duration.ofMillis(86400000L));
  }

  @Test
  @DisplayName("RefreshToken 조회 테스트")
  void getTest() {
    when(valueOps.get("refresh_token:" + userId)).thenReturn(refreshToken);

    String result = refreshTokenStore.get(userId);
    assertEquals(refreshToken, result);
  }

  @Test
  @DisplayName("RefreshToken 삭제 테스트")
  void removeTest() {
    refreshTokenStore.remove(userId);

    verify(redisTemplate).delete("refresh_token:" + userId);
  }
}
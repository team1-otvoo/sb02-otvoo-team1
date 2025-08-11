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
class RedisAccessTokenStoreTest {

  @Mock
  private StringRedisTemplate redisTemplate;

  @Mock
  private ValueOperations<String, String> valueOps;

  @InjectMocks
  private RedisAccessTokenStore accessTokenStore;

  private UUID userId;
  private String accessToken;

  @BeforeEach
  void setup() {
    userId = UUID.randomUUID();
    accessToken = "testAccessToken";

    lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
  }

  @Test
  @DisplayName("AccessToken 저장 테스트")
  void saveTest() {
    accessTokenStore.save(userId, accessToken, 3600);

    verify(valueOps).set("access_token:" + userId, accessToken, Duration.ofSeconds(3600));
  }

  @Test
  @DisplayName("AccessToken 조회 테스트")
  void getTest() {
    when(valueOps.get("access_token:" + userId)).thenReturn(accessToken);

    String result = accessTokenStore.get(userId);
    assertEquals(accessToken, result);
  }

  @Test
  @DisplayName("AccessToken 삭제 테스트")
  void removeTest() {
    accessTokenStore.remove(userId);

    verify(redisTemplate).delete("access_token:" + userId);
  }

  @Test
  @DisplayName("블랙리스트 등록 테스트")
  void blacklistAccessTokenTest() {
    accessTokenStore.blacklistAccessToken(accessToken, 3600);

    verify(valueOps).set("jwt:blacklist:" + accessToken, "1", Duration.ofSeconds(3600));
  }

  @Test
  @DisplayName("블랙리스트 확인 테스트 - 존재할 때")
  void isBlacklisted_TrueTest() {
    when(redisTemplate.hasKey("jwt:blacklist:" + accessToken)).thenReturn(true);

    assertTrue(accessTokenStore.isBlacklisted(accessToken));
  }

  @Test
  @DisplayName("블랙리스트 확인 테스트 - 없을 때")
  void isBlacklisted_FalseTest() {
    when(redisTemplate.hasKey("jwt:blacklist:" + accessToken)).thenReturn(false);

    assertFalse(accessTokenStore.isBlacklisted(accessToken));
  }
}
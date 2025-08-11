package com.team1.otvoo.auth.token;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class RedisTemporaryPasswordStoreTest {

  @Mock
  private StringRedisTemplate redisTemplate;

  @Mock
  private ValueOperations<String, String> valueOps;

  @InjectMocks
  private RedisTemporaryPasswordStore temporaryPasswordStore;

  private UUID userId;
  private TemporaryPassword tempPassword;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setup() {
    userId = UUID.randomUUID();
    tempPassword = new TemporaryPassword("tempPwd1234", System.currentTimeMillis() + 10000);
    lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
  }

  @Test
  @DisplayName("임시 비밀번호 저장 테스트")
  void saveTest() {
    temporaryPasswordStore.save(userId, tempPassword, Duration.ofSeconds(10));

    String expectedKey = "temp-pwd:" + userId.toString();
    verify(valueOps).set(eq(expectedKey), anyString());
  }

  @Test
  @DisplayName("임시 비밀번호 조회 테스트")
  void getTest() throws Exception {
    String json = objectMapper.writeValueAsString(tempPassword);
    when(valueOps.get("temp-pwd:" + userId.toString())).thenReturn(json);

    TemporaryPassword result = temporaryPasswordStore.get(userId);

    assertNotNull(result);
    assertEquals(tempPassword.getPassword(), result.getPassword());
    assertEquals(tempPassword.getExpirationTime(), result.getExpirationTime());
  }

  @Test
  @DisplayName("임시 비밀번호 조회 실패 - null 반환 테스트")
  void getNullTest() {
    when(valueOps.get("temp-pwd:" + userId.toString())).thenReturn(null);

    TemporaryPassword result = temporaryPasswordStore.get(userId);
    assertNull(result);
  }

  @Test
  @DisplayName("임시 비밀번호 삭제 테스트")
  void removeTest() {
    temporaryPasswordStore.remove(userId);

    verify(redisTemplate).delete("temp-pwd:" + userId.toString());
  }
}
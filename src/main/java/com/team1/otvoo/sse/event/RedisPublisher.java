package com.team1.otvoo.sse.event;

import com.team1.otvoo.sse.model.SseMessage;
import com.team1.otvoo.sse.repository.SseEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisPublisher {

  private final RedisTemplate<String, Object> redisTemplate;
  private final SseEventRepository sseEventRepository;

  public void publish(SseMessage message) {
    // 재전송을 위해 이벤트를 Redis에 캐싱
    sseEventRepository.save(message);
    // 'sse-channel'이라는 채널로 SseMessage를 발행
    redisTemplate.convertAndSend("sse-channel", message);
  }

}

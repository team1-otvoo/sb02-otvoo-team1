package com.team1.otvoo.sse.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team1.otvoo.sse.model.SseMessage;
import com.team1.otvoo.sse.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStreamListener implements
    StreamListener<String, ObjectRecord<String, String>> {

  private final ObjectMapper objectMapper;
  private final SseService sseService;

  @Override
  public void onMessage(ObjectRecord<String, String> record) {
    try {
      String json = record.getValue();
      SseMessage message = objectMapper.readValue(json, SseMessage.class);

      log.info("Redis Stream에서 메시지 수신: {}", message.getEventData());
      sseService.sendEvent(message);
    } catch (Exception e) {
      log.error("Listener 처리 실패: {}", record.getValue(), e);
    }
  }
}
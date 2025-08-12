package com.team1.otvoo.sse.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team1.otvoo.sse.model.SseMessage;
import com.team1.otvoo.sse.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class RedisSubscriber {

  private final ObjectMapper objectMapper;
  private final SseService sseService;

  public void handleMessage(SseMessage sseMessage) {
      log.info("Redis Subscribed Message: {}", sseMessage.getEventData());
      sseService.sendEvent(sseMessage);
  }

}

package com.team1.otvoo.sse.service;

import com.team1.otvoo.sse.model.SseMessage;
import java.util.UUID;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseService {
  SseEmitter connect(UUID userId, UUID lastEventId);
  void sendEvent(SseMessage sseMessage);
}

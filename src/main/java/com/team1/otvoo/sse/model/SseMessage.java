package com.team1.otvoo.sse.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SseMessage {

  private UUID eventId;
  @Builder.Default
  private Set<UUID> receiverIds = new HashSet<>();
  @Builder.Default
  private boolean broadcast = false;
  private String eventName;
  private Object eventData;

  public boolean isReceivable(UUID receiverId) {
    return broadcast || receiverIds.contains(receiverId);
  }

  public SseEmitter.SseEventBuilder toSseEventBuilder() {
    return SseEmitter.event()
        .id(eventId.toString())
        .name(eventName)
        .data(eventData);
  }

}

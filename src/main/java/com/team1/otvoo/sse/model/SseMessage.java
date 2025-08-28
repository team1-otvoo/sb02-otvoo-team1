package com.team1.otvoo.sse.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SseMessage {

  private UUID eventId;
  @Builder.Default
  private Set<UUID> receiverIds = new HashSet<>();
  @Builder.Default
  private boolean broadcast = false;
  private String eventName;
  private Object eventData;
  @Builder.Default
  private Instant createdAt = Instant.now();

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

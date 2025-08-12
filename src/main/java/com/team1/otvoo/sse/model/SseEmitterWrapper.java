package com.team1.otvoo.sse.model;

import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class SseEmitterWrapper {

  @EqualsAndHashCode.Include
  private final UUID emitterId;
  private final SseEmitter emitter;
  private final Instant createdAt;

  public static SseEmitterWrapper wrap(SseEmitter emitter) {
    return new SseEmitterWrapper(UUID.randomUUID(), emitter, Instant.now());
  }

}

package com.team1.otvoo.sse.repository;

import com.team1.otvoo.sse.model.SseEmitterWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class SseEmitterRepository {

  private final Map<UUID, Map<UUID, SseEmitterWrapper>> userEmitters  = new ConcurrentHashMap<>();

  public SseEmitterWrapper save(UUID userId, SseEmitterWrapper emitterWrapper) {
    userEmitters.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
        .put(emitterWrapper.getEmitterId(), emitterWrapper);
    log.debug("Emitter 저장: {} -> {}", userId, emitterWrapper.getEmitterId());
    return emitterWrapper;
  }

  public void delete(UUID userId, SseEmitterWrapper emitterWrapper) {
    UUID emitterId = emitterWrapper.getEmitterId();
    userEmitters.computeIfPresent(userId, (key, emitters) -> {
      emitters.remove(emitterId);
      if (emitters.isEmpty()) {
        return null;
      }
      return emitters;
    });
    log.debug("Emitter 삭제: emitterId={}", emitterId);
  }

  public List<SseEmitterWrapper> findAllByReceiverId(UUID userId) {
    return new ArrayList<>(userEmitters.getOrDefault(userId, Map.of()).values());
  }

  public void forEach(BiConsumer<UUID, SseEmitterWrapper> action) {
    userEmitters.forEach((userId, emitters) -> {
      emitters.values().forEach(wrapper -> action.accept(userId, wrapper));
    });
  }

}

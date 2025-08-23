package com.team1.otvoo.sse.repository;

import com.team1.otvoo.sse.model.SseEmitterWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class SseEmitterRepository {

  private final Map<UUID, Map<UUID, SseEmitterWrapper>> userEmitters  = new ConcurrentHashMap<>();
  private final AtomicInteger connectionCount = new AtomicInteger(0);

  @Value("${sse.max-connections}")
  private int maxConnections;

  public SseEmitterWrapper save(UUID userId, SseEmitterWrapper emitterWrapper) {
    int count = connectionCount.incrementAndGet();
    if (count > maxConnections) {
      connectionCount.decrementAndGet(); // 롤백
      throw new RuntimeException("최대 연결 수를 초과했습니다.");
    }

    userEmitters.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
        .put(emitterWrapper.getEmitterId(), emitterWrapper);
    log.debug("Emitter 저장: {} -> {}", userId, emitterWrapper.getEmitterId());
    return emitterWrapper;
  }

  public void delete(UUID userId, SseEmitterWrapper emitterWrapper) {
    UUID emitterId = emitterWrapper.getEmitterId();
    userEmitters.computeIfPresent(userId, (key, emitters) -> {
      SseEmitterWrapper removedEmitter = emitters.remove(emitterId);

      if (removedEmitter != null) {
        connectionCount.decrementAndGet();
        log.debug("Emitter 삭제 성공: userId={}, emitterId={}, 현재 연결 수={}",
            userId, emitterId, connectionCount.get());
      }
      return emitters.isEmpty() ? null : emitters;
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

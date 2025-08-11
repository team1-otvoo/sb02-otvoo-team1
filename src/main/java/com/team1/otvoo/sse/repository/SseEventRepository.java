package com.team1.otvoo.sse.repository;

import com.team1.otvoo.sse.model.SseMessage;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class SseEventRepository {

  @Value("${sse.event-ttl-seconds}")
  private long eventTilSeconds;

  private final RedisTemplate<String, Object> redisTemplate;

  private static final String EVENT_STREAM_KEY = "sse:events"; // Sorted Set 키
  private static final String EVENT_DATA_KEY_PREFIX = "sse:event_data:"; // Hash 키 접두사

  // 새로운 이벤트 저장
  public void save(SseMessage sseMessage) {
    String eventId = sseMessage.getEventId().toString();
    long now = System.currentTimeMillis();

    redisTemplate.opsForZSet().add(EVENT_STREAM_KEY, eventId, now);

    String eventDataKey = EVENT_DATA_KEY_PREFIX + eventId;
    redisTemplate.opsForValue().set(eventDataKey, sseMessage, eventTilSeconds, TimeUnit.SECONDS);
  }

  // 유실된 데이터 조회
  // 최대 이벤트 개수 제한 등 다른 전략 추가할지 고민
  public List<SseMessage> findAllByEventIdAfterAndReceiverId(UUID lastEventId, UUID receiverId) {
    // lastEventId에 해당하는 score(시간) 찾기
    Double lastEventScore = redisTemplate.opsForZSet().score(EVENT_STREAM_KEY, lastEventId.toString());

    if (lastEventScore == null) {
      return List.of();
    }

    // lastEventId 이후의 모든 eventId를 조회
    Set<Object> eventIds = redisTemplate.opsForZSet().rangeByScore(EVENT_STREAM_KEY, lastEventScore, Double.MAX_VALUE);

    if (eventIds == null || eventIds.isEmpty()) {
      return List.of();
    }

    // MGET을 위한 키 목록 생성
    List<String> eventDataKeys = eventIds.stream()
        .map(id -> EVENT_DATA_KEY_PREFIX + id.toString())
        .toList();

    // MGET으로 한번에 데이터 조회
    List<Object> eventDataList = Optional.ofNullable(
            redisTemplate.opsForValue().multiGet(eventDataKeys))
        .orElse(Collections.emptyList());

    // 실제 데이터 조회
    // score가 같을 경우 eventId의 사전 순(lexicographical order)으로 정렬
    // 현재 로직은 lastEventId와 같은 score를 가진 모든 이벤트를 가져온 뒤 lastEventId만 제외
    // → 순서는 약간 바뀔 수 있지만, 같은 시간에 발생한 다른 이벤트가 누락되지는 않음
    // 이벤트 순서를 더 엄격하게 보장하고 싶으면 필터 조건 추가 고려해볼 것 (ex. createdAt)
    return eventDataList.stream()
        .filter(Objects::nonNull)
        .map(data -> (SseMessage) data)
        .filter(sseMessage -> !sseMessage.getEventId().equals(lastEventId))
        .filter(sseMessage -> sseMessage.isReceivable(receiverId))
        .toList();
  }

}

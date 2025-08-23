package com.team1.otvoo.sse.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team1.otvoo.sse.model.SseMessage;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisStreamService {

  private final StringRedisTemplate stringRedisTemplate;
  private final ObjectMapper objectMapper;

  private static final String STREAM_KEY = "sse-stream";
  private static final String INDEX_KEY_PREFIX = "sse:index:";

  @Value("${stream.index-ttl-days:7}")
  private long INDEX_TTL_DAYS;

  @Value("${stream.max-stream-length:100000}")
  private long MAX_STREAM_LENGTH;

  @Value("${stream.retention-days:7}")
  private long RETENTION_DAYS;

  private static final DefaultRedisScript<String> XADD_AND_SET_SCRIPT;
  static {
    String lua =
        "local id = redis.call('XADD', KEYS[1], '*', 'payload', ARGV[1]); " +
            "redis.call('SET', ARGV[2], id); " +
            "redis.call('PEXPIRE', ARGV[2], ARGV[3]); " +
            "return id;";
    XADD_AND_SET_SCRIPT = new DefaultRedisScript<>(lua, String.class);
  }

  /**
   * 메시지 발행
   * - Redis Lua 스크립트를 사용하여 메시지 발행(XADD)과 인덱스 저장(SET)을 원자적으로 처리
   * - 인덱스 키와 메시지 ID를 연결하여 나중에 특정 이벤트 ID의 메시지를 빠르게 찾을 수 있게함
   */
  public void publish(SseMessage message) {
    try {
      String json = objectMapper.writeValueAsString(message);
      String indexKey = INDEX_KEY_PREFIX + message.getEventId().toString();
      String ttlMs = String.valueOf(TimeUnit.DAYS.toMillis(INDEX_TTL_DAYS));

      // Lua 스크립트 실행. KEYS와 ARGV 배열에 전달할 인자를 순서대로 지정
      String recordId = stringRedisTemplate.execute(
          XADD_AND_SET_SCRIPT,
          Collections.singletonList(STREAM_KEY),
          json, indexKey, ttlMs
      );

      log.info("Redis Stream 발행 성공. Record ID: {}, Index Key: {}", recordId, indexKey);
    } catch (Exception e) {
      log.error("Redis Stream 메시지 발행 실패", e);
    }
  }

  /**
   * 유실된 데이터 조회
   * - 클라이언트가 마지막으로 받은 메시지 ID를 기반으로, 그 이후에 발행된 메시지를 찾아 복구
   * - 인덱스(`sse:index:UUID`)를 이용해 스트림 메시지 ID를 빠르게 조회
   */
  public List<SseMessage> findAllByEventIdAfterAndReceiverId(UUID lastEventId, UUID receiverId) {
    if (lastEventId == null) return Collections.emptyList();

    String indexKey = INDEX_KEY_PREFIX + lastEventId;
    String lastRecordId = stringRedisTemplate.opsForValue().get(indexKey);
    if (lastRecordId == null) {
      log.warn("인덱스에서 RecordId를 찾을 수 없음: lastEventId={}", lastEventId);
      return Collections.emptyList();
    }

    try {
      List<ObjectRecord<String, String>> records = stringRedisTemplate.opsForStream().read(
          String.class,
          StreamReadOptions.empty().count(1000),
          StreamOffset.create(STREAM_KEY, ReadOffset.from(RecordId.of(lastRecordId)))
      );

      return records.stream()
          .map(r -> {
            try {
              return objectMapper.readValue(r.getValue(), SseMessage.class);
            } catch (Exception ex) {
              log.error("SseMessage 역직렬화 실패 기록: recordId={}, value={}", r.getId().getValue(), r.getValue(), ex);
              return null;
            }
          })
          .filter(Objects::nonNull)
          .filter(msg -> !msg.getEventId().equals(lastEventId))
          .filter(msg -> msg.isReceivable(receiverId))
          .toList();

    } catch (Exception e) {
      log.error("Stream 조회 실패. lastRecordId: {}", lastRecordId, e);
      return Collections.emptyList();
    }
  }

  /**
   * Redis Stream 정리 (Trim)
   * 1. 길이 기준: 스트림의 길이가 MAX_STREAM_LENGTH를 초과하면 오래된 메시지를 자동으로 삭제
   * 2. 시간 기준: 특정 시간(RETENTION_DAYS)보다 오래된 메시지 삭제
   * - `cutoffId`를 계산하여 MINID를 지정하면, Redis가 해당 ID보다 작은(오래된) 모든 메시지를 삭제
   */
  @Scheduled(cron = "0 0 5 * * *")
  public void trimStream() {
    try {
      if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(STREAM_KEY))) {
        log.info("Stream [{}]이 존재하지 않아 trim을 건너뜁니다.", STREAM_KEY);
        return;
      }

      Long lengthBefore = stringRedisTemplate.opsForStream().size(STREAM_KEY);

      // 1) 길이 기준 trim (MAXLEN)
      stringRedisTemplate.opsForStream().trim(STREAM_KEY, MAX_STREAM_LENGTH);

      // 2) 시간 기준 삭제 (MINID)
      long cutoffTs = Instant.now().minus(RETENTION_DAYS, ChronoUnit.DAYS).toEpochMilli();
      String cutoffId = cutoffTs + "-0"; // 해당 시간의 가장 이른 ID (순서 번호 0)

      Long deletedByTime = stringRedisTemplate.execute((RedisCallback<Long>) connection -> {
        byte[] rawKey = stringRedisTemplate.getStringSerializer().serialize(STREAM_KEY);
        byte[] rawId = stringRedisTemplate.getStringSerializer().serialize(cutoffId);

        // XTRIM key MINID cutoffId
        return (Long) connection.execute("XTRIM", rawKey, "MINID".getBytes(StandardCharsets.UTF_8), rawId);
      });

      Long lengthAfter = stringRedisTemplate.opsForStream().size(STREAM_KEY);

      log.info("Redis Stream [{}] trim 완료. 길이: {} -> {}. MAXLEN 삭제: {}, TIME 삭제: {}",
          STREAM_KEY, lengthBefore, lengthAfter, Math.max(0, lengthBefore - deletedByTime - lengthAfter), deletedByTime);

    } catch (Exception e) {
      log.error("Redis Stream trim 실패", e);
    }
  }
}

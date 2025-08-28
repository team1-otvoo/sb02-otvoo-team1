package com.team1.otvoo.sse.event;

import com.team1.otvoo.notification.entity.AsyncTaskFailure;
import com.team1.otvoo.notification.repository.AsyncTaskFailureRepository;
import com.team1.otvoo.sse.model.SseMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPublishService {

  private final RedisStreamService redisStreamService;
  private final AsyncTaskFailureRepository asyncTaskFailureRepository;

  @Retryable(
      value = { RuntimeException.class },
      maxAttempts = 3,
      backoff = @Backoff(delay = 1000, multiplier = 2, random = true) // 지터(Jitter)를 적용하여 재시도 충돌 방지
  )
  public void publishWithRetry(SseMessage message) {
    log.info("Redis Stream 발행 시도: EventId={}", message.getEventId());
    redisStreamService.publish(message);
  }

  @Recover
  public void recover(RuntimeException e, SseMessage message) {
    log.error("Redis Stream 발행 최종 실패: EventId={}, ReceiverIds={}. 원인: {}",
        message.getEventId(), message.getReceiverIds(), e.getMessage());

    // 실패 내역을 DB에 저장
    AsyncTaskFailure failure = AsyncTaskFailure.builder()
        .eventId(message.getEventId())
        .payload(message.getEventData() != null ? message.getEventData().toString() : "N/A")
        .error(e.getMessage())
        .build();

    asyncTaskFailureRepository.save(failure);
  }

}

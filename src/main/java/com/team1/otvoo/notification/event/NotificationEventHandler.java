package com.team1.otvoo.notification.event;

import com.team1.otvoo.notification.dto.NotificationDto;
import com.team1.otvoo.sse.event.RedisPublishService;
import com.team1.otvoo.sse.model.SseMessage;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventHandler {

  private final RedisPublishService redisPublishService;

  @Async
  @TransactionalEventListener
  public void handleEvent(NotificationEvent event) {
    try {
      for(NotificationDto notificationDto : event.notificationDtoList()) {
        SseMessage message = createSseMessage(notificationDto, event.broadcast());
        redisPublishService.publishWithRetry(message);

        log.info("Redis Stream 메시지 발행 완료: {}", message.getEventData());
      }
    } catch (Exception e) {
      log.error("NotificationEvent 처리 중 예외 발생: {}", e.getMessage(), e);
    }
  }

  private SseMessage createSseMessage(NotificationDto notificationDto, boolean broadcast) {

    log.info("메시지 전환 전 notificationDto {}, 브로드캐스트 여부: {}", notificationDto, broadcast);
    SseMessage.SseMessageBuilder builder = SseMessage.builder()
        .eventId(UUID.randomUUID())
        .broadcast(broadcast)
        .eventName("notifications")
        .eventData(notificationDto)
        .createdAt(Instant.now());

    if (!broadcast && notificationDto.receiverId() != null) {
      builder.receiverIds(Set.of(notificationDto.receiverId()));
    }

    return builder.build();
  }

}

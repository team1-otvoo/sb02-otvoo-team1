package com.team1.otvoo.feed.event;

import com.team1.otvoo.notification.service.SendNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeedEventHandler {

  private final SendNotificationService SendNotificationService;

  @Async
  @TransactionalEventListener
  public void handleEvent(FeedEvent event) {
    try {
      SendNotificationService.sendFeedNotification(event.savedFeed());
    } catch (Exception e) {
      log.error("피드 알림 전송 실패: {}", e.getMessage(), e);
    }
  }

  @Async
  @TransactionalEventListener
  public void handleLikeEvent(FeedLikeEvent event) {
    try {
      SendNotificationService.sendLikeNotification(event.savedFeedLike());
    } catch (Exception e) {
      log.error("좋아요 알림 전송 실패: {}", e.getMessage(), e);
    }
  }

}

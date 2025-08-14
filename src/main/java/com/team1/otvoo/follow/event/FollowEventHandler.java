package com.team1.otvoo.follow.event;

import com.team1.otvoo.notification.service.SendNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class FollowEventHandler {

  private final SendNotificationService SendNotificationService;

  @Async
  @TransactionalEventListener
  public void handleEvent(FollowEvent event) {
    try {
      SendNotificationService.sendFollowNotification(event.follower(), event.followee());
    } catch (Exception e) {
      log.error("팔로우 알림 전송 실패: {}", e.getMessage(), e);
    }
  }

}

package com.team1.otvoo.directmessage.event;

import com.team1.otvoo.notification.service.SendNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DirectMessageEventHandler {

  private final SendNotificationService SendNotificationService;

  @Async
  @TransactionalEventListener
  public void handleEvent(DirectMessageEvent event) {
    try {
      SendNotificationService.sendDirectMessageNotification(event.savedDirectMessage());
    } catch (Exception e) {
      log.error("DM 알림 전송 실패: {}", e.getMessage(), e);
    }
  }

}

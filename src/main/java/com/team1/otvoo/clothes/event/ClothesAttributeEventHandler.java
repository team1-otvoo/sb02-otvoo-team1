package com.team1.otvoo.clothes.event;

import com.team1.otvoo.notification.service.SendNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClothesAttributeEventHandler {

  private final SendNotificationService SendNotificationService;

  @Async
  @TransactionalEventListener
  public void handleEvent(ClothesAttributeEvent event) {
    try{
      SendNotificationService.sendClothesAttributeNotification(event.methodType(), event.savedClothesAttributeDefinition());
    } catch (Exception e) {
      log.error("의상 속성 알림 전송 실패: {}", e.getMessage(), e);
    }
  }

}

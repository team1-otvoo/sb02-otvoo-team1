package com.team1.otvoo.user.event;

import com.team1.otvoo.sqs.MessageSenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileImageEventListener {

  private final MessageSenderService messageSenderService;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleProfileImageUploaded(ProfileImageUploadedEvent event) {
    log.info("DB 커밋 완료, SQS 메시지 발행 시작: {}", event.objectKey());
    messageSenderService.sendImageResizeMessage(event.objectKey(), event.width(), event.height());
  }
}
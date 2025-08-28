package com.team1.otvoo.directmessage.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.BDDMockito.*;

import com.team1.otvoo.directmessage.entity.DirectMessage;
import com.team1.otvoo.notification.service.SendNotificationService;
import com.team1.otvoo.user.entity.User;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DirectMessageEventHandlerTest {

  @InjectMocks
  private DirectMessageEventHandler directMessageEventHandler;

  @Mock
  private SendNotificationService sendNotificationService;

  @Mock
  private User sender;

  @Mock
  private User receiver;

  private DirectMessageEvent directMessageEvent;
  private DirectMessage directMessage;

  @BeforeEach
  void setUp() {
    directMessage = DirectMessage.builder()
        .id(UUID.randomUUID())
        .sender(sender)
        .receiver(receiver)
        .content("테스트 DM")
        .createdAt(Instant.now())
        .build();

    directMessageEvent = new DirectMessageEvent(directMessage);
  }

  @Nested
  @DisplayName("DM 이벤트 처리 테스트")
  class HandleDirectMessageEventTests {

    @Test
    @DisplayName("성공_DM 이벤트 발생 시 알림 서비스 호출")
    void handleEvent_Success_ShouldCallNotificationService() {
      // when
      directMessageEventHandler.handleEvent(directMessageEvent);

      // then
      then(sendNotificationService).should().sendDirectMessageNotification(directMessage);
    }

    @Test
    @DisplayName("실패_알림 서비스에서 예외 발생 시에도 핸들러는 정상 종료")
    void handleEvent_Failure_ShouldCompleteGracefully() {
      // given
      willThrow(new RuntimeException("알림 전송 실패"))
          .given(sendNotificationService).sendDirectMessageNotification(any(DirectMessage.class));

      // when & then
      assertDoesNotThrow(() -> directMessageEventHandler.handleEvent(directMessageEvent));
      then(sendNotificationService).should().sendDirectMessageNotification(directMessage);
    }
  }
}

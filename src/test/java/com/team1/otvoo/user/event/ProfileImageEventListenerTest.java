package com.team1.otvoo.user.event;

import static org.mockito.Mockito.verify;

import com.team1.otvoo.sqs.MessageSenderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("ProfileImageEventListener 단위 테스트")
@ExtendWith(MockitoExtension.class)
class ProfileImageEventListenerTest {

  @InjectMocks
  private ProfileImageEventListener profileImageEventListener;

  @Mock
  private MessageSenderService messageSenderService;

  @Test
  @DisplayName("ProfileImageUploadedEvent 수신 시 SQS 메시지 전송을 요청한다")
  void should_requestSqsMessageSend_when_eventIsHandled() {
    // given
    String testObjectKey = "profile/images/user123.png";
    int testWidth = 800;
    int testHeight = 600;
    ProfileImageUploadedEvent event = new ProfileImageUploadedEvent(testObjectKey, testWidth, testHeight);

    // when
    // 이벤트 리스너의 핸들러 메서드를 직접 호출
    profileImageEventListener.handleProfileImageUploaded(event);

    // then
    // messageSenderService의 sendImageResizeMessage 메서드가
    // 이벤트에 담겨 있던 정확한 인자들로 호출되었는지 검증
    verify(messageSenderService).sendImageResizeMessage(testObjectKey, testWidth, testHeight);
  }
}
package com.team1.otvoo.follow.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import com.team1.otvoo.notification.service.SendNotificationService;
import com.team1.otvoo.user.entity.User;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class FollowEventHandlerTest {

  @InjectMocks
  private FollowEventHandler followEventHandler;

  @Mock
  private SendNotificationService sendNotificationService;

  private User follower;
  private User followee;
  private FollowEvent followEvent;

  @BeforeEach
  void setUp() {
    UUID followerId = UUID.randomUUID();
    UUID followeeId = UUID.randomUUID();

    follower = spy(new User("follower@test.com", "password123"));
    ReflectionTestUtils.setField(follower, "id", followerId);

    followee = spy(new User("followee@test.com", "password123"));
    ReflectionTestUtils.setField(followee, "id", followeeId);

    followEvent = new FollowEvent(follower, followee);
  }

  @Nested
  @DisplayName("팔로우 이벤트 처리 테스트")
  class HandleFollowEventTests {

    @Test
    @DisplayName("성공_팔로우 이벤트 발생 시 알림 서비스 호출")
    void handleEvent_Success_ShouldCallNotificationService() {
      // when
      followEventHandler.handleEvent(followEvent);

      // then
      then(sendNotificationService).should().sendFollowNotification(follower, followee);
    }

    @Test
    @DisplayName("실패_알림 서비스에서 예외 발생 시에도 핸들러는 정상 종료")
    void handleEvent_Failure_ShouldCompleteEvenWhenNotificationServiceThrows() {
      // given
      doThrow(new RuntimeException("알림 전송 실패"))
          .when(sendNotificationService).sendFollowNotification(follower, followee);

      // when & then
      assertDoesNotThrow(() -> followEventHandler.handleEvent(followEvent));
      then(sendNotificationService).should().sendFollowNotification(follower, followee);
    }
  }
}
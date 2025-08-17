package com.team1.otvoo.user.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.BDDMockito.*;

import com.team1.otvoo.notification.service.SendNotificationService;
import com.team1.otvoo.user.entity.Role;
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
class UserRoleEventHandlerTest {

  @InjectMocks
  private UserRoleEventHandler userRoleEventHandler;

  @Mock
  private SendNotificationService sendNotificationService;

  private UserRoleEvent userRoleEvent;
  private User user;
  private final Role previousRole = Role.USER;

  @BeforeEach
  void setUp() {
    user = spy(new User("test@test.com", "password123"));
    ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

    userRoleEvent = new UserRoleEvent(previousRole, user);
  }

  @Nested
  @DisplayName("사용자 권한 변경 이벤트 처리 테스트")
  class HandleUserRoleEventTests {

    @Test
    @DisplayName("성공_권한 변경 이벤트 발생 시 알림 서비스 호출")
    void handleEvent_Success_ShouldCallNotificationService() {
      // when
      userRoleEventHandler.handleEvent(userRoleEvent);

      // then
      then(sendNotificationService).should().sendUserRoleNotification(previousRole, user);
    }

    @Test
    @DisplayName("실패_알림 서비스에서 예외 발생 시에도 핸들러는 정상 종료")
    void handleEvent_Failure_ShouldCompleteGracefully() {
      // given
      willThrow(new RuntimeException("알림 전송 실패"))
          .given(sendNotificationService).sendUserRoleNotification(any(Role.class), any(User.class));

      // when & then
      assertDoesNotThrow(() -> userRoleEventHandler.handleEvent(userRoleEvent));
      then(sendNotificationService).should().sendUserRoleNotification(previousRole, user);
    }
  }
}

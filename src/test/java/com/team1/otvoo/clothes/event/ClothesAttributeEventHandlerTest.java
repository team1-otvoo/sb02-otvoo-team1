package com.team1.otvoo.clothes.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.BDDMockito.*;

import com.team1.otvoo.clothes.entity.ClothesAttributeDefinition;
import com.team1.otvoo.notification.service.SendNotificationService;
import java.util.Collections;
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
class ClothesAttributeEventHandlerTest {

  @InjectMocks
  private ClothesAttributeEventHandler clothesAttributeEventHandler;

  @Mock
  private SendNotificationService sendNotificationService;

  private ClothesAttributeEvent clothesAttributeEvent;
  private ClothesAttributeDefinition definition;
  private final String methodType = "CREATE";

  @BeforeEach
  void setUp() {
    definition = spy(new ClothesAttributeDefinition("색상", Collections.emptyList()));
    ReflectionTestUtils.setField(definition, "id", UUID.randomUUID());

    clothesAttributeEvent = new ClothesAttributeEvent(methodType, definition);
  }

  @Nested
  @DisplayName("의상 속성 이벤트 처리 테스트")
  class HandleClothesAttributeEventTests {

    @Test
    @DisplayName("성공_의상 속성 이벤트 발생 시 알림 서비스 호출")
    void handleEvent_Success_ShouldCallNotificationService() {
      // when
      clothesAttributeEventHandler.handleEvent(clothesAttributeEvent);

      // then
      then(sendNotificationService).should().sendClothesAttributeNotification(methodType, definition);
    }

    @Test
    @DisplayName("실패_알림 서비스에서 예외 발생 시에도 핸들러는 정상 종료")
    void handleEvent_Failure_ShouldCompleteGracefully() {
      // given
      willThrow(new RuntimeException("알림 전송 실패"))
          .given(sendNotificationService).sendClothesAttributeNotification(anyString(), any(ClothesAttributeDefinition.class));

      // when & then
      assertDoesNotThrow(() -> clothesAttributeEventHandler.handleEvent(clothesAttributeEvent));
      then(sendNotificationService).should().sendClothesAttributeNotification(methodType, definition);
    }
  }
}

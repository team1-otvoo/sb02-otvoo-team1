package com.team1.otvoo.sqs;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team1.otvoo.sqs.dto.ImageResizeData;
import com.team1.otvoo.sqs.dto.SqsMessageDto;
import com.team1.otvoo.sqs.dto.TaskPayload;
import com.team1.otvoo.sqs.dto.TaskType;
import com.team1.otvoo.sqs.task.TaskHandler;
import com.team1.otvoo.sqs.task.TaskHandlerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("MessageReceiver 단위 테스트")
@ExtendWith(MockitoExtension.class)
class MessageReceiverTest {

  @InjectMocks // @Mock 객체들을 주입받을 테스트 대상
  private MessageReceiver messageReceiver;

  @Mock // 가짜 객체로 만들 의존성
  private TaskHandlerFactory taskHandlerFactory;

  @Mock // 팩토리가 반환할 가짜 핸들러 객체
  private TaskHandler mockTaskHandler;

  private SqsMessageDto testMessage;
  private TaskPayload data;

  // --- 👇 [수정] @BeforeEach 추가 ---
  @BeforeEach
  void setUp() {
    // 각 테스트가 실행되기 전에 공통 데이터를 초기화합니다.
    data = new ImageResizeData("key", 10, 10);
    testMessage = new SqsMessageDto(data);
  }
  // ---------------------------------

  @Nested
  @DisplayName("정상적인 메시지 수신 시")
  class Context_WithMessageSuccess {
    @Test
    @DisplayName("적절한 핸들러를 찾아 작업을 위임한다")
    void should_delegateTask_to_correctHandler() {
      // given
      given(taskHandlerFactory.getHandler(TaskType.IMAGE_RESIZE)).willReturn(mockTaskHandler);

      // when
      messageReceiver.receiveMessage(testMessage); // 이제 testMessage는 null이 아님

      // then
      verify(taskHandlerFactory).getHandler(TaskType.IMAGE_RESIZE);
      verify(mockTaskHandler).handle(data);
    }
  }

  @Nested
  @DisplayName("지원하지 않는 TaskType의 메시지 수신 시")
  class Context_WithUnsupportedTaskType {
    @Test
    @DisplayName("핸들러를 찾지 못하고 메시지를 폐기한다")
    void should_discardMessage_when_handlerNotFound() {
      // given
      given(taskHandlerFactory.getHandler(TaskType.IMAGE_RESIZE)).willReturn(null);

      // when
      messageReceiver.receiveMessage(testMessage); // 이제 testMessage는 null이 아님

      // then
      verify(taskHandlerFactory).getHandler(TaskType.IMAGE_RESIZE);
      verify(mockTaskHandler, never()).handle(any(TaskPayload.class));
    }
  }

  @Nested
  @DisplayName("메시지 처리 중 핸들러에서 예외 발생 시")
  class Context_WithHandlerException {
    @Test
    @DisplayName("SQS 재시도를 위해 RuntimeException을 다시 던진다")
    void should_rethrowRuntimeException_for_sqsRetry() {
      // given
      given(taskHandlerFactory.getHandler(TaskType.IMAGE_RESIZE)).willReturn(mockTaskHandler);
      doThrow(new RuntimeException("DB connection failed"))
          .when(mockTaskHandler)
          .handle(any(TaskPayload.class));

      // when & then
      assertThrows(RuntimeException.class, () -> {
        messageReceiver.receiveMessage(testMessage); // 이제 testMessage는 null이 아님
      });

      // then
      verify(mockTaskHandler).handle(data);
    }
  }
}
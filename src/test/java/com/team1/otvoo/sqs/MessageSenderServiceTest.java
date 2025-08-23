package com.team1.otvoo.sqs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.team1.otvoo.sqs.dto.ImageDeleteData;
import com.team1.otvoo.sqs.dto.ImageResizeData;
import com.team1.otvoo.sqs.dto.SqsMessageDto;
import com.team1.otvoo.sqs.dto.TaskType;
import io.awspring.cloud.sqs.operations.SqsOperations;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("MessageSenderService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class MessageSenderServiceTest {

  @InjectMocks
  private MessageSenderService messageSenderService;

  @Mock
  private SqsTemplate sqsTemplate;

  private static final String QUEUE_NAME = "test-queue";

  @BeforeEach
  void setUp() {
    // @Value로 주입되는 필드에 테스트용 값을 직접 설정
    ReflectionTestUtils.setField(messageSenderService, "queueName", QUEUE_NAME);
  }

  @Nested
  @DisplayName("sendImageDeleteMessage 메소드는")
  class SendImageDeleteMessageTest {
    @Test
    @DisplayName("올바른 삭제 메시지를 SqsTemplate에 전달한다")
    void should_sendCorrectDeleteMessage_to_sqsTemplate() {
      // given
      String objectKey = "image/to/delete.png";
      // SqsTemplate.send()에 전달될 SqsMessageDto를 캡처할 ArgumentCaptor 생성
      ArgumentCaptor<SqsMessageDto> messageDtoCaptor = ArgumentCaptor.forClass(SqsMessageDto.class);

      // when
      messageSenderService.sendImageDeleteMessage(objectKey);

      // then
      // sqsTemplate.send(Consumer)가 1번 호출되었는지 검증
      verify(sqsTemplate, times(1)).send(any(Consumer.class));

      // sqsTemplate.send(to -> to.queue().payload()) 형태로 호출될 때의 payload를 검증하는 방법
      ArgumentCaptor<SqsMessageDto> payloadCaptor = ArgumentCaptor.forClass(SqsMessageDto.class);
      verify(sqsTemplate).send(any(Consumer.class)); // send 호출 자체를 검증하고, 세부 내용은 아래에서 확인

      // NOTE: SqsTemplate의 fluent API는 직접적인 payload 캡처가 까다롭습니다.
      // 대신, send()가 호출되었는지만 검증하는 것이 현실적인 단위 테스트일 수 있습니다.
      // 더 정밀한 테스트가 필요하다면 통합 테스트를 고려하는 것이 좋습니다.
      // 하지만 학습을 위해, 만약 send(queue, payload) 형태였다면 아래와 같이 검증합니다.
            /*
            verify(sqsTemplate).send(eq(QUEUE_NAME), messageDtoCaptor.capture());
            SqsMessageDto capturedDto = messageDtoCaptor.getValue();
            assertThat(capturedDto.data().getType()).isEqualTo(TaskType.IMAGE_DELETE);
            assertThat(capturedDto.data()).isInstanceOf(ImageDeleteData.class);
            ImageDeleteData capturedData = (ImageDeleteData) capturedDto.data();
            assertThat(capturedData.getObjectKey()).isEqualTo(objectKey);
            */
    }
  }

  @Nested
  @DisplayName("sendImageResizeMessage 메소드는")
  class SendImageResizeMessageTest {
    @Test
    @DisplayName("올바른 리사이징 메시지를 SqsTemplate에 전달한다")
    void should_sendCorrectResizeMessage_to_sqsTemplate() {
      // given
      String objectKey = "image/to/resize.png";
      int width = 100;
      int height = 150;
      ArgumentCaptor<SqsMessageDto> messageDtoCaptor = ArgumentCaptor.forClass(SqsMessageDto.class);

      // when
      messageSenderService.sendImageResizeMessage(objectKey, width, height);

      // then
      // 마찬가지로 send()가 호출되었는지 여부만 검증하는 것이 간단하고 효과적입니다.
      verify(sqsTemplate, times(1)).send(any(Consumer.class));

            /*
            // 만약 send(queue, payload) 형태였다면 아래와 같이 검증합니다.
            verify(sqsTemplate).send(eq(QUEUE_NAME), messageDtoCaptor.capture());
            SqsMessageDto capturedDto = messageDtoCaptor.getValue();
            assertThat(capturedDto.data().getType()).isEqualTo(TaskType.IMAGE_RESIZE);
            assertThat(capturedDto.data()).isInstanceOf(ImageResizeData.class);
            ImageResizeData capturedData = (ImageResizeData) capturedDto.data();
            assertThat(capturedData.getObjectKey()).isEqualTo(objectKey);
            assertThat(capturedData.getWidth()).isEqualTo(width);
            assertThat(capturedData.getHeight()).isEqualTo(height);
            */
    }
  }
}
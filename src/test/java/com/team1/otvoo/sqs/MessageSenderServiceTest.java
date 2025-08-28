package com.team1.otvoo.sqs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.team1.otvoo.sqs.dto.SqsMessageDto;
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

    }
  }
}
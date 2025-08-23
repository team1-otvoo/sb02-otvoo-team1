package com.team1.otvoo.sqs.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

import com.team1.otvoo.sqs.dto.ImageDeleteData;
import com.team1.otvoo.sqs.dto.ImageResizeData;
import com.team1.otvoo.sqs.dto.TaskPayload;
import com.team1.otvoo.sqs.dto.TaskType;
import com.team1.otvoo.storage.ImageProcessingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("ImageResizeHandler 단위테스트")
@ExtendWith(MockitoExtension.class)
class ImageResizeHandlerTest {

  @InjectMocks
  private ImageResizeHandler imageResizeHandler;

  @Mock
  private ImageProcessingService imageProcessingService;

  @Nested
  @DisplayName("handle 메소드는")
  class HandleTest {

    @Test
    @DisplayName("올바른 타입의 payload를 받으면 imageProcessingService.resizeAndOverwrite 를 호출한다")

    void should_callS3Delete_with_correctObjectKey()  {
      // given
      String objectKey = "path/to/image.png";
      TaskPayload payload = new ImageResizeData(objectKey, 10, 10);

      // when
      imageResizeHandler.handle(payload);

      // then
      verify(imageProcessingService).resizeAndOverwrite(objectKey, 10, 10);
    }

    @Test
    @DisplayName("잘못된 타입의 payload를 받으면 ClassCastException을 던진다")
    void should_throwClassCastException_when_invalidPayloadIsPassed() {
      // given
      TaskPayload invalidPayload = new ImageDeleteData("key");

      // when & then
      // handle 메서드 실행 시 ClassCastException이 발생하는지 검증
      assertThrows(ClassCastException.class, () -> {
        imageResizeHandler.handle(invalidPayload);
      });
    }
  }

  @Test
  @DisplayName("getSupportType 메소드는 IMAGE_DELETE 타입을 반환한다")
  void should_returnCorrectSupportType() {
    // when & then
    assertThat(imageResizeHandler.getSupportType()).isEqualTo(TaskType.IMAGE_RESIZE);
  }



}
package com.team1.otvoo.sqs.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

import com.team1.otvoo.sqs.dto.ImageDeleteData;
import com.team1.otvoo.sqs.dto.ImageResizeData;
import com.team1.otvoo.sqs.dto.TaskPayload;
import com.team1.otvoo.sqs.dto.TaskType;
import com.team1.otvoo.storage.S3ImageStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("ImageDeleteHandler 단위 테스트")
@ExtendWith(MockitoExtension.class)
class ImageDeleteHandlerTest {

  @InjectMocks
  private ImageDeleteHandler imageDeleteHandler;

  @Mock
  private S3ImageStorage s3ImageStorage;

  @Nested
  @DisplayName("handle 메소드는")
  class HandleTest {

    @Test
    @DisplayName("올바른 타입의 payload를 받으면 S3ImageStorage.delete를 호출한다")
    void should_callS3Delete_with_correctObjectKey() {
      // given
      String objectKey = "path/to/image.png";
      TaskPayload payload = new ImageDeleteData(objectKey);

      // when
      imageDeleteHandler.handle(payload);

      // then
      // s3ImageStorage.delete가 objectKey를 인자로 정확히 1번 호출되었는지 검증
      verify(s3ImageStorage).delete(objectKey);
    }

    @Test
    @DisplayName("잘못된 타입의 payload를 받으면 ClassCastException을 던진다")
    void should_throwClassCastException_when_invalidPayloadIsPassed() {
      // given
      // ImageDeleteData가 아닌 다른 타입의 payload를 생성
      TaskPayload invalidPayload = new ImageResizeData("key", 100, 100);

      // when & then
      // handle 메서드 실행 시 ClassCastException이 발생하는지 검증
      assertThrows(ClassCastException.class, () -> {
        imageDeleteHandler.handle(invalidPayload);
      });
    }
  }

  @Test
  @DisplayName("getSupportType 메소드는 IMAGE_DELETE 타입을 반환한다")
  void should_returnCorrectSupportType() {
    // when & then
    assertThat(imageDeleteHandler.getSupportType()).isEqualTo(TaskType.IMAGE_DELETE);
  }
}
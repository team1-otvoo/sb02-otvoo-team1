package com.team1.otvoo.storage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ResourceUtils;

@ExtendWith(MockitoExtension.class) // JUnit 5에서 Mockito를 사용하기 위한 설정
class ImageProcessingServiceTest {

  @InjectMocks // @Mock으로 생성된 가짜 객체들을 이 객체에 주입합니다.
  private ImageProcessingService imageProcessingService;

  @Mock // 가짜(Mock) 객체를 생성합니다.
  private S3ImageStorage s3ImageStorage;

  @Test
  @DisplayName("이미지 리사이징 및 덮어쓰기 성공")
  void resizeAndOverwrite_Success() throws IOException { // IOException 대신 Exception으로 변경하거나 try-catch를 사용
    // given - 테스트 준비
    String objectKey = "test_image.png";
    int width = 100;
    int height = 100;

    // --- 이 부분이 변경됩니다 ---
    // 1. 테스트 리소스 폴더에 있는 진짜 이미지 파일을 읽어옵니다.
    // "classpath:"는 src/test/resources/ 경로를 가리킵니다.
    Path imagePath = ResourceUtils.getFile("classpath:images/test_image.png").toPath();
    byte[] realImageBytes = Files.readAllBytes(imagePath);
    // ------------------------

    // s3ImageStorage.download(objectKey)가 호출되면, 실제 이미지 바이트를 반환하도록 설정
    given(s3ImageStorage.download(objectKey)).willReturn(realImageBytes);

    // when - 테스트 대상 메서드 실행
    assertDoesNotThrow(() -> imageProcessingService.resizeAndOverwrite(objectKey, width, height));

    // then - 결과 검증
    verify(s3ImageStorage).download(eq(objectKey));
    verify(s3ImageStorage).upload(
        eq(objectKey),
        any(InputStream.class),
        anyLong(),
        eq("image/png")
    );
  }

  @Test
  @DisplayName("S3 다운로드 실패 시 RuntimeException 발생")
  void resizeAndOverwrite_DownloadFails_ThrowsException() throws IOException {
    // given - 테스트 준비
    String objectKey = "test_image.jpg";
    int width = 100;
    int height = 100;

    // s3ImageStorage.download(objectKey)가 호출되면, IOException을 던지도록 설정
    given(s3ImageStorage.download(objectKey)).willThrow(new IOException("S3 다운로드 실패"));

    // when & then - 테스트 대상 메서드 실행 시 특정 예외가 발생하는지 검증
    assertThrows(RuntimeException.class, () -> {
      imageProcessingService.resizeAndOverwrite(objectKey, width, height);
    });

    // then - 추가 검증
    // upload는 호출되지 않았어야 함
    verify(s3ImageStorage, Mockito.never()).upload(anyString(), any(InputStream.class), anyLong(), anyString());
  }
}
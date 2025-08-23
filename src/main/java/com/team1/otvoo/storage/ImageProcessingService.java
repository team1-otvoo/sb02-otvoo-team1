package com.team1.otvoo.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ImageProcessingService {

  private final S3ImageStorage s3ImageStorage;
  private static final String OUTPUT_FORMAT = "png";

  // 비동기로 처리될 이미지 리사이징 및 덮어쓰기 로직
  public void resizeAndOverwrite(String objectKey, int width, int height) {
    try {
      log.info("이미지 리사이즈 시도 - objectKey: " + objectKey);

      // 1. S3에서 원본 이미지를 byte 배열로 안전하게 다운로드
      byte[] originalImageBytes = s3ImageStorage.download(objectKey);

      // 2. 메모리 상에서 이미지 리사이징
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      Thumbnails.of(new ByteArrayInputStream(originalImageBytes)) // byte[]를 다시 InputStream으로 변환
          .size(width, height)
          .keepAspectRatio(true)
          .outputFormat(OUTPUT_FORMAT)
          .toOutputStream(outputStream);
      byte[] resizedBytes = outputStream.toByteArray();

      // 3. 리사이징된 이미지로 S3 원본 덮어쓰기 (기존 upload 메서드 활용)
      s3ImageStorage.upload(
          objectKey,
          new ByteArrayInputStream(resizedBytes),
          resizedBytes.length,
          "image/" + OUTPUT_FORMAT
      );

    } catch (IOException e) {
      log.error("이미지 리사이징 실패: {}", objectKey, e);
      throw new RuntimeException("Image resizing failed", e);
    }
  }
}
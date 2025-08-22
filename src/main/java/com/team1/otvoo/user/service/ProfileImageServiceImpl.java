package com.team1.otvoo.user.service;

import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.sqs.MessageSenderService;
import com.team1.otvoo.storage.S3ImageStorage;
import com.team1.otvoo.user.dto.ProfileImageMetaData;
import com.team1.otvoo.user.entity.Profile;
import com.team1.otvoo.user.entity.ProfileImage;
import com.team1.otvoo.user.event.ProfileImageUploadedEvent;
import com.team1.otvoo.user.mapper.ProfileImageMapper;
import com.team1.otvoo.user.repository.ProfileImageRepository;
import com.team1.otvoo.user.resolver.ProfileImageUrlResolver;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProfileImageServiceImpl implements ProfileImageService {

  private final ProfileImageRepository profileImageRepository;
  private final ProfileImageUrlResolver profileImageUrlResolver;
  private final S3ImageStorage s3ImageStorage;
  private final Tika tika;

  private final MessageSenderService messageSenderService;

  private final ProfileImageMapper profileImageMapper;

  private final ApplicationEventPublisher eventPublisher; // 이벤트 발행기 주입

  @Override
  @Transactional
  public String replaceProfileImageAndGetUrl(Profile profile, @Nullable MultipartFile file) {
    UUID profileId = profile.getId();

    if (file == null || file.isEmpty()) {
      return profileImageUrlResolver.resolve(profileId);
    }

    try {
      // 1. S3에 업로드하기 전, 파일 유효성 검사 및 메타데이터 추출
      //    (createProfileImageAndMetaData 메서드는 아래에 새로 정의)
      PreparedProfileImage preparedImage = prepareProfileImage(file, profile);
      ProfileImage newProfileImage = preparedImage.profileImage();
      ProfileImageMetaData metaData = profileImageMapper.profileImageToProfileImageMetaData(newProfileImage);

      // 2. DB 작업 먼저 수행
      profileImageRepository.findByProfileId(profileId)
          .ifPresentOrElse(
              image -> image.updateMetaData(metaData),
              () -> profileImageRepository.save(newProfileImage)
          );

      // 3. S3에 파일 업로드 (DB 작업 이후)
      s3ImageStorage.upload(
          metaData.objectKey(),
          preparedImage.inputStream(), // 준비된 InputStream 사용
          metaData.size(),
          metaData.contentType()
      );

      // 4. 모든 작업 성공 후 메시지 전송 이벤트 발행 (이벤트로 처리한 이유는 트랜잭션 롤백시 sqs 에 message 전송도 롤백되어야 하기 때문
      eventPublisher.publishEvent(
          new ProfileImageUploadedEvent(metaData.objectKey(), metaData.width(), metaData.height())
      );

      // 5. Presigned URL 반환
      return s3ImageStorage.getPresignedUrl(metaData.objectKey(), metaData.contentType());

    } catch (IOException e) {
      log.error("파일 처리 중 오류 발생", e);
      // 이 예외가 발생하면 @Transactional에 의해 DB 작업이 롤백
      throw new RestException(ErrorCode.PROFILE_IMAGE_UPLOAD_FAILED);
    }
  }

  // S3 업로드를 제외하고 엔티티와 스트림을 준비하는 메서드
  private PreparedProfileImage prepareProfileImage(MultipartFile file, Profile profile) throws IOException {

    // 크기 검증
    long maxSize = 10 * 1024 * 1024; // 10MB
    if (file.getSize() > maxSize) {
      throw new RestException(ErrorCode.TOO_BIG_PROFILE_IMAGE, Map.of("message", "파일 크기가 10MB 이상입니다."));
    }

    // 1. 파일을 byte 배열로 변환하여 여러 번 읽을 수 있도록 준비
    byte[] fileBytes = file.getBytes();
    int width;
    int height;

    // 실제 파일 타입을 감지
    String mimeType = tika.detect(fileBytes);
    if (!mimeType.startsWith("image/")) {
      throw new RestException(ErrorCode.INVALID_PROFILE_IMAGE_TYPE);
    }

    // 2. 원본 파일 유효성 검사 (이미지 타입, 크기 등)
    try (InputStream inputStreamForValidation = new ByteArrayInputStream(fileBytes)) {
      BufferedImage image = ImageIO.read(inputStreamForValidation);
      if (image == null) {
        throw new RestException(ErrorCode.INVALID_PROFILE_IMAGE_TYPE);
      }
      width = image.getWidth();
      height = image.getHeight();

      if (width > 5000 || height > 5000) {
        throw new RestException(ErrorCode.TOO_BIG_PROFILE_IMAGE);
      }
    }

    String key = "images/profileImages/" + profile.getId().toString() + ".png";

    ProfileImage profileImage = new ProfileImage(
        key,
        file.getOriginalFilename(),
        file.getContentType(),
        (long) fileBytes.length,
        width,
        height,
        profile
    );

    InputStream inputStreamForUpload = new ByteArrayInputStream(fileBytes);
    return new PreparedProfileImage(profileImage, inputStreamForUpload);
  }

  // 데이터를 함께 전달하기 위한 간단한 내부 클래스
    private record PreparedProfileImage(
        ProfileImage profileImage,
        InputStream inputStream
  ) {

  }
}

package com.team1.otvoo.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.storage.S3ImageStorage;
import com.team1.otvoo.user.dto.ProfileImageMetaData;
import com.team1.otvoo.user.entity.Profile;
import com.team1.otvoo.user.entity.ProfileImage;
import com.team1.otvoo.user.event.ProfileImageUploadedEvent;
import com.team1.otvoo.user.mapper.ProfileImageMapper;
import com.team1.otvoo.user.repository.ProfileImageRepository;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;
import org.apache.tika.Tika;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ProfileImageServiceImplTest {

  @InjectMocks
  private ProfileImageServiceImpl profileImageService;

  // 모든 의존성을 @Mock으로 선언
  @Mock
  private ProfileImageRepository profileImageRepository;
  @Mock
  private S3ImageStorage s3ImageStorage;
  @Mock
  private Tika tika;
  @Mock
  private ProfileImageMapper profileImageMapper;
  @Mock
  private ApplicationEventPublisher eventPublisher;

  private Profile profile;
  private MockMultipartFile imageFile;
  private ProfileImageMetaData metaData;

  @BeforeEach
  void setUp() throws IOException { // IOException을 던지도록 수정
    // 테스트에서 공통으로 사용할 객체들 설정
    profile = new Profile("testUser", null);
    com.team1.otvoo.user.mapper.TestUtils.setField(profile, "id", UUID.randomUUID());

    // 1. src/test/resources 폴더의 실제 이미지 파일을 읽어옵니다.
    ClassPathResource resource = new ClassPathResource("images/test_image.png");
    byte[] imageBytes = resource.getInputStream().readAllBytes();

    // 2. 실제 이미지 파일의 바이트 배열로 MockMultipartFile을 생성합니다.
    imageFile = new MockMultipartFile(
        "image",
        "test.png",
        "image/png",
        imageBytes
    );

    metaData = new ProfileImageMetaData(
        "images/profileImages/" + profile.getId() + ".png",
        "test.png",
        "image/png",
        (long) imageBytes.length,
        100, // 실제 이미지의 너비/높이와는 다를 수 있지만, 이 테스트의 핵심은 아님
        200
    );
  }

  @Nested
  @DisplayName("새로운 이미지 업로드 시 (기존 이미지 없음)")
  class WhenNewImageIsUploaded {

    @Test
    @DisplayName("성공적으로 DB에 저장하고 S3에 업로드 후 Presigned URL을 반환한다")
    void shouldSaveAndUploadImageAndReturnUrl() throws IOException {
      // given
      // 1. 의존성 Mocking 설정
      when(tika.detect(any(byte[].class))).thenReturn("image/png"); // tika가 이미지 파일이라고 응답하도록
      when(profileImageRepository.findByProfileId(profile.getId())).thenReturn(Optional.empty()); // DB에 기존 이미지가 없다고 응답
      when(profileImageMapper.profileImageToProfileImageMetaData(any(ProfileImage.class))).thenReturn(metaData);
      when(s3ImageStorage.getPresignedUrl(metaData.objectKey(), metaData.contentType())).thenReturn("presigned-url");

      // when
      String resultUrl = profileImageService.replaceProfileImageAndGetUrl(profile, imageFile);

      // then
      // 2. 결과 검증
      assertThat(resultUrl).isEqualTo("presigned-url");

      // 3. 상호작용(행위) 검증
      // 'save'가 한 번 호출되었는지 검증
      verify(profileImageRepository, times(1)).save(any(ProfileImage.class));
      // S3에 업로드가 한 번 호출되었는지 검증
      verify(s3ImageStorage, times(1)).upload(anyString(), any(InputStream.class), anyLong(), anyString());
      // 이벤트가 한 번 발행되었는지 검증
      verify(eventPublisher, times(1)).publishEvent(any(ProfileImageUploadedEvent.class));
    }
  }

  @Nested
  @DisplayName("기존 이미지 교체 시 (기존 이미지 있음)")
  class WhenImageIsReplaced {

    @Test
    @DisplayName("성공적으로 DB 메타데이터를 업데이트하고 S3에 업로드 후 Presigned URL을 반환한다")
    void shouldUpdateAndUploadImageAndReturnUrl() throws IOException {
      // given
      ProfileImage existingImage = Mockito.spy(
          new ProfileImage("old-key", "old.png", "image/png", 1L, 10, 10, profile)
      );
      when(tika.detect(any(byte[].class))).thenReturn("image/png");
      when(profileImageRepository.findByProfileId(profile.getId())).thenReturn(Optional.of(existingImage));
      when(profileImageMapper.profileImageToProfileImageMetaData(any(ProfileImage.class))).thenReturn(metaData);
      when(s3ImageStorage.getPresignedUrl(metaData.objectKey(), metaData.contentType())).thenReturn("presigned-url-updated");

      // when
      String resultUrl = profileImageService.replaceProfileImageAndGetUrl(profile, imageFile);

      // then
      assertThat(resultUrl).isEqualTo("presigned-url-updated");

      // ArgumentCaptor를 사용하여 updateMetaData에 전달된 인자 캡처
      ArgumentCaptor<ProfileImageMetaData> captor = ArgumentCaptor.forClass(ProfileImageMetaData.class);
      verify(existingImage, times(1)).updateMetaData(captor.capture());
      assertThat(captor.getValue().objectKey()).isEqualTo(metaData.objectKey());

      verify(profileImageRepository, never()).save(any(ProfileImage.class));
      verify(s3ImageStorage, times(1)).upload(anyString(), any(InputStream.class), anyLong(), anyString());
      verify(eventPublisher, times(1)).publishEvent(any(ProfileImageUploadedEvent.class));
    }
  }

  @Test
  @DisplayName("파일이 이미지가 아닐 경우 RestException을 던진다")
  void shouldThrowException_whenFileIsNotImage() throws IOException {
    // given
    MockMultipartFile notImageFile = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());
    when(tika.detect(any(byte[].class))).thenReturn("text/plain");

    // when & then
    assertThatThrownBy(() -> profileImageService.replaceProfileImageAndGetUrl(profile, notImageFile))
        .isInstanceOf(RestException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PROFILE_IMAGE_TYPE);
  }
}
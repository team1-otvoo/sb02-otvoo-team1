package com.team1.otvoo.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.user.entity.Profile;
import com.team1.otvoo.user.entity.ProfileImage;
import com.team1.otvoo.user.mapper.TestUtils;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class ProfileImageServiceImplTest {

  @InjectMocks
  private ProfileImageServiceImpl profileImageService;

  @Test
  @DisplayName("정상적인 이미지 파일이면 ProfileImage를 생성한다")
  void createProfileImage_success() throws IOException {
    // given
    BufferedImage bufferedImage = new BufferedImage(100, 200, BufferedImage.TYPE_INT_RGB);
    var baos = new java.io.ByteArrayOutputStream();
    ImageIO.write(bufferedImage, "jpg", baos);
    byte[] imageBytes = baos.toByteArray();

    MultipartFile multipartFile = new MockMultipartFile(
        "image", "test.jpg", "image/jpeg", imageBytes
    );

    Profile profile = new Profile("홍길동", null);
    UUID profileId = UUID.randomUUID();
    TestUtils.setField(profile, "id", profileId);

    // when
    ProfileImage result = profileImageService.createProfileImage(multipartFile, profile);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getImageUrl()).isEqualTo("s3Uploader.upload");
    assertThat(result.getOriginalFilename()).isEqualTo("test.jpg");
    assertThat(result.getContentType()).isEqualTo("image/jpeg");
    assertThat(result.getSize()).isEqualTo(imageBytes.length);
    assertThat(result.getWidth()).isEqualTo(100);
    assertThat(result.getHeight()).isEqualTo(200);
    assertThat(result.getProfile()).isEqualTo(profile);
  }

  @Test
  @DisplayName("이미지 읽기 실패 시 RestException이 발생한다")
  void createProfileImage_ioException() throws IOException {
    // given
    MultipartFile brokenFile = Mockito.mock(MultipartFile.class);
    Mockito.when(brokenFile.getOriginalFilename()).thenReturn("broken.png");
    Mockito.when(brokenFile.getContentType()).thenReturn("image/png");
    Mockito.when(brokenFile.getSize()).thenReturn(123L);
    Mockito.when(brokenFile.getInputStream()).thenThrow(new IOException("읽기 실패"));

    Profile profile = new Profile("홍길동", null);
    UUID profileId = UUID.randomUUID();
    TestUtils.setField(profile, "id", profileId);

    // when & then
    assertThatThrownBy(() -> profileImageService.createProfileImage(brokenFile, profile))
        .isInstanceOf(RestException.class)
        .hasMessage(ErrorCode.IO_EXCEPTION.getMessage())
        .extracting("errorCode")
        .isEqualTo(ErrorCode.IO_EXCEPTION);
  }
}
package com.team1.otvoo.user.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.team1.otvoo.config.props.DefaultProfileImageProperties;
import com.team1.otvoo.storage.S3ImageStorage;
import com.team1.otvoo.user.entity.Profile; // Profile import 추가
import com.team1.otvoo.user.entity.ProfileImage;
import com.team1.otvoo.user.repository.ProfileImageRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileImageUrlResolverTest {

  @InjectMocks
  private ProfileImageUrlResolver resolver;

  @Mock
  private ProfileImageRepository profileImageRepository;

  @Mock
  private DefaultProfileImageProperties defaultProfileImageProperties;

  @Mock
  private S3ImageStorage s3ImageStorage;

  @Test
  @DisplayName("프로필 이미지가 존재할 경우 S3 Presigned URL을 반환한다")
  void resolve_returnsPresignedUrl_whenProfileImageExists() {
    // given
    UUID profileId = UUID.randomUUID();
    String expectedUrl = "https://s3.ap-northeast-2.amazonaws.com/otvoo/presigned-url";

    // 1. ProfileImage가 의존하는 Profile 객체를 먼저 생성합니다.
    //    테스트 목적이므로 실제 DB에 있는 User 객체까지는 필요 없습니다. null을 전달해도 무방합니다.
    Profile ownerProfile = new Profile("test-user", null);

    // 2. 생성한 Profile 객체를 사용하여 ProfileImage를 생성합니다.
    ProfileImage profileImage = new ProfileImage(
        "images/profile/" + UUID.randomUUID(),
        "original.png",
        "image/png",
        1024L,
        500,
        500,
        ownerProfile // 생성자에 Profile 객체 전달
    );

    // findByProfileId가 Optional<ProfileImage>를 반환하도록 스텁
    when(profileImageRepository.findByProfileId(profileId))
        .thenReturn(Optional.of(profileImage));

    // s3ImageStorage.getPresignedUrl 호출 시 예상 URL을 반환하도록 스텁
    when(s3ImageStorage.getPresignedUrl(profileImage.getObjectKey(), profileImage.getContentType()))
        .thenReturn(expectedUrl);

    // when
    String result = resolver.resolve(profileId);

    // then
    assertThat(result).isEqualTo(expectedUrl);
  }

  @Test
  @DisplayName("프로필 이미지가 존재하지 않으면 기본 URL을 반환한다")
  void resolve_returnsDefaultUrl_whenNoProfileImage() {
    // given
    UUID profileId = UUID.randomUUID();
    String defaultUrl = "https://cdn.example.com/default.png";

    // findByProfileId가 빈 Optional을 반환하도록 스텁
    when(profileImageRepository.findByProfileId(profileId))
        .thenReturn(Optional.empty());

    // 기본 이미지 URL 프로퍼티 스텁
    when(defaultProfileImageProperties.getUrl()).thenReturn(defaultUrl);

    // when
    String result = resolver.resolve(profileId);

    // then
    assertThat(result).isEqualTo(defaultUrl);
  }
}
package com.team1.otvoo.user.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.team1.otvoo.config.props.DefaultProfileImageProperties;
import com.team1.otvoo.user.entity.ProfileImage;
import com.team1.otvoo.user.repository.ProfileImageRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileImageUrlResolverTest {

  @InjectMocks
  private ProfileImageUrlResolver resolver;

  @Mock
  private ProfileImageRepository profileImageRepository;
  @Mock
  private DefaultProfileImageProperties defaultImageProperties;

  @Test
  @DisplayName("프로필 이미지가 존재할 경우 해당 URL을 반환한다")
  void resolve_returnsStoredImageUrl_whenProfileImageExists() {
    // given
    UUID profileId = UUID.randomUUID();
    String imageUrl = "https://cdn.example.com/profile.jpg";
    ProfileImage profileImage = Mockito.mock(ProfileImage.class);
    when(profileImage.getImageUrl()).thenReturn(imageUrl);
    when(profileImageRepository.findByProfileId(profileId)).thenReturn(Optional.of(profileImage));

    // when
    String result = resolver.resolve(profileId);

    // then
    assertThat(result).isEqualTo(imageUrl);
  }

  @Test
  @DisplayName("프로필 이미지가 존재하지 않으면 기본 URL을 반환한다")
  void resolve_returnsDefaultUrl_whenNoProfileImage() {
    // given
    UUID profileId = UUID.randomUUID();
    String defaultUrl = "https://cdn.example.com/default.png";

    when(profileImageRepository.findByProfileId(profileId)).thenReturn(Optional.empty());
    when(defaultImageProperties.getUrl()).thenReturn(defaultUrl);

    // when
    String result = resolver.resolve(profileId);

    // then
    assertThat(result).isEqualTo(defaultUrl);
  }
}
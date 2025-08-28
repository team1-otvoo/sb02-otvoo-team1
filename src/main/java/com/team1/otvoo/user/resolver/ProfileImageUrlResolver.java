package com.team1.otvoo.user.resolver;

import com.team1.otvoo.config.props.DefaultProfileImageProperties;
import com.team1.otvoo.storage.S3ImageStorage;
import com.team1.otvoo.user.repository.ProfileImageRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProfileImageUrlResolver {

  private final ProfileImageRepository profileImageRepository;
  private final DefaultProfileImageProperties defaultProfileImageProperties;
  private final S3ImageStorage s3ImageStorage;

  public String resolve(UUID profileId) {
    return profileImageRepository.findByProfileId(profileId)
        .map(image -> s3ImageStorage.getPresignedUrl(
            image.getObjectKey(),
            image.getContentType())
        ).orElse(defaultProfileImageProperties.getUrl()); // 없으면 기본값 제공
  }
}
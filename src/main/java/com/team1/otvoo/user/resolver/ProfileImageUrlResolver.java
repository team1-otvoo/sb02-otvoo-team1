package com.team1.otvoo.user.resolver;

import com.team1.otvoo.config.props.DefaultProfileImageProperties;
import com.team1.otvoo.user.entity.ProfileImage;
import com.team1.otvoo.user.repository.ProfileImageRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProfileImageUrlResolver {

  private final ProfileImageRepository profileImageRepository;
  private final DefaultProfileImageProperties defaultProfileImageProperties;

  public String resolve(UUID profileId) {
    return profileImageRepository.findByProfileId(profileId)
        .map(ProfileImage::getImageUrl)
        .orElse(defaultProfileImageProperties.getUrl());
  }
}

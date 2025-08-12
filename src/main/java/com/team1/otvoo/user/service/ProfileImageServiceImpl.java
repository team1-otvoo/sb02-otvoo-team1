package com.team1.otvoo.user.service;

import com.team1.otvoo.config.props.DefaultProfileImageProperties;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.user.entity.Profile;
import com.team1.otvoo.user.entity.ProfileImage;
import com.team1.otvoo.user.repository.ProfileImageRepository;
import com.team1.otvoo.user.resolver.ProfileImageUrlResolver;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@Service
public class ProfileImageServiceImpl implements ProfileImageService {

  private final ProfileImageRepository profileImageRepository;
  private final ProfileImageUrlResolver profileImageUrlResolver;
  private final DefaultProfileImageProperties defaultImageProperties;

  @Override
  public ProfileImage createProfileImage(MultipartFile file, Profile profile) {
    String imageUrl = "s3Uploader.upload";

    String originalFilename = file.getOriginalFilename();
    String contentType = file.getContentType();
    long size = file.getSize();

    int width = 0;
    int height = 0;

    try (InputStream inputStream = file.getInputStream()) {
      BufferedImage image = ImageIO.read(inputStream);
      if (image != null) {
        width = image.getWidth();
        height = image.getHeight();
      }
    } catch (IOException e) {
      throw new RestException(
          ErrorCode.IO_EXCEPTION,
          Map.of("다음 프로필 아이디의 이미지 처리 중 오류가 발생했습니다.", profile.getId())
      );
    }

    return new ProfileImage(
        imageUrl,
        originalFilename,
        contentType,
        size,
        width,
        height,
        profile
    );
  }

  @Override
  public void deleteProfileImage(ProfileImage profileImage) {
    // Transactional Event 발행?
  }

  @Override
  @Transactional
  public String replaceProfileImageAndGetUrl(Profile profile, @Nullable MultipartFile file) {
    UUID profileId = profile.getId();

    if (file == null || file.isEmpty()) {
      // 읽기 전용 URL 해석도 여기서 책임지게 할 수 있음
      return profileImageUrlResolver.resolve(profileId);
    }

    // 기존 이미지 업데이트
    ProfileImage newImage = createProfileImage(file, profile); // 저장소 업로드 + 엔티티 생성
    Optional<ProfileImage> oldImage = profileImageRepository.findByProfileId(profileId);

    if (oldImage.isPresent()) {
      ProfileImage profileImage = oldImage.get();
      profileImage.updateFrom(newImage);
    } else {
      profileImageRepository.save(newImage);
    }

    return newImage.getImageUrl();
  }
}

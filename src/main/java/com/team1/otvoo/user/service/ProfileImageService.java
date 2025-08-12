package com.team1.otvoo.user.service;

import com.team1.otvoo.user.entity.Profile;
import com.team1.otvoo.user.entity.ProfileImage;
import org.springframework.lang.Nullable;
import org.springframework.web.multipart.MultipartFile;

public interface ProfileImageService {
  ProfileImage createProfileImage(MultipartFile file, Profile profile);
  void deleteProfileImage(ProfileImage profileImage);
  String replaceProfileImageAndGetUrl(Profile profile, @Nullable MultipartFile file);
}

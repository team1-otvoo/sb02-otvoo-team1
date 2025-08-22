package com.team1.otvoo.user.service;

import com.team1.otvoo.user.entity.Profile;
import org.springframework.lang.Nullable;
import org.springframework.web.multipart.MultipartFile;

public interface ProfileImageService {
  String replaceProfileImageAndGetUrl(Profile profile, @Nullable MultipartFile file);
}

package com.team1.otvoo.user.service;

import com.team1.otvoo.user.dto.ChangePasswordRequest;
import com.team1.otvoo.user.dto.ProfileDto;
import com.team1.otvoo.user.dto.ProfileUpdateRequest;
import com.team1.otvoo.user.dto.UserCreateRequest;
import com.team1.otvoo.user.dto.UserDto;
import com.team1.otvoo.user.dto.UserDtoCursorRequest;
import com.team1.otvoo.user.dto.UserDtoCursorResponse;
import com.team1.otvoo.user.dto.UserLockUpdateRequest;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

  UserDtoCursorResponse getUsers(UserDtoCursorRequest request);
  UserDto createUser(UserCreateRequest userCreateRequest);
  ProfileDto getUserProfile(UUID userId);
  ProfileDto updateProfile(UUID userId, ProfileUpdateRequest profileUpdateRequest, MultipartFile profileImageFile);
  void changePassword(UUID userId, ChangePasswordRequest request);
  UUID changeLock(UUID userId, UserLockUpdateRequest request);
}

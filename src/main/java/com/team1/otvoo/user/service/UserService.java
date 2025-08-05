package com.team1.otvoo.user.service;

import com.team1.otvoo.user.dto.ChangePasswordRequest;
import com.team1.otvoo.user.dto.ProfileDto;
import com.team1.otvoo.user.dto.UserCreateRequest;
import com.team1.otvoo.user.dto.UserDto;
import com.team1.otvoo.user.dto.UserDtoCursorRequest;
import com.team1.otvoo.user.dto.UserDtoCursorResponse;
import java.util.UUID;

public interface UserService {

  UserDtoCursorResponse getUsers(UserDtoCursorRequest request);
  UserDto createUser(UserCreateRequest userCreateRequest);
  ProfileDto getUserProfile(UUID userId);
  void changePassword(UUID userId, ChangePasswordRequest request);

}

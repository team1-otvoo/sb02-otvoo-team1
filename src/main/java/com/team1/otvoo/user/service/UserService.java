package com.team1.otvoo.user.service;

import com.team1.otvoo.user.dto.ChangePasswordRequest;
import com.team1.otvoo.user.dto.UserCreateRequest;
import com.team1.otvoo.user.dto.UserDto;
import jakarta.validation.Valid;
import java.util.UUID;

public interface UserService {

  UserDto createUser(UserCreateRequest userCreateRequest);
  void changePassword(UUID userId, ChangePasswordRequest request);

}

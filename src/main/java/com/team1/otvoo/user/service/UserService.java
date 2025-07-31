package com.team1.otvoo.user.service;

import com.team1.otvoo.user.dto.UserCreateRequest;
import com.team1.otvoo.user.dto.UserDto;

public interface UserService {

  UserDto createUser(UserCreateRequest userCreateRequest);

}

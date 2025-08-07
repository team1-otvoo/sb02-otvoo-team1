package com.team1.otvoo.user.repository;

import com.team1.otvoo.user.dto.UserDtoCursorRequest;
import com.team1.otvoo.user.dto.UserSlice;

public interface UserRepositoryCustom {
  UserSlice searchUsersWithCursor(UserDtoCursorRequest request);
}

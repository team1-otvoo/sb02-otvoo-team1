package com.team1.otvoo.user.repository;

import com.team1.otvoo.user.dto.AuthorDto;
import com.team1.otvoo.user.dto.UserDtoCursorRequest;
import com.team1.otvoo.user.dto.UserRow;
import com.team1.otvoo.user.dto.UserSlice;
import java.util.UUID;

public interface UserRepositoryCustom {
  UserSlice<UserRow> searchUserRowWithCursor(UserDtoCursorRequest request);
  AuthorDto projectionAuthorDtoById(UUID userId);
}

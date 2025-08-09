package com.team1.otvoo.user.repository;

import com.team1.otvoo.user.dto.AuthorDto;
import com.team1.otvoo.user.dto.UserDtoCursorRequest;
import com.team1.otvoo.user.dto.UserSlice;
import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryCustom {
  UserSlice searchUsersWithCursor(UserDtoCursorRequest request);
  AuthorDto projectionAuthorDtoById(UUID userId);
}

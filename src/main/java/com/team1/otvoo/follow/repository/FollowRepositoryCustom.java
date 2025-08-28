package com.team1.otvoo.follow.repository;

import com.team1.otvoo.follow.dto.FollowCursorDto;
import com.team1.otvoo.follow.dto.FollowDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FollowRepositoryCustom {
  FollowDto findFollowDtoById(UUID followId);
  List<FollowCursorDto> findFollowingsWithCursor(UUID followerId, Instant cursor, UUID idAfter, int limit, String nameLike);
  List<FollowCursorDto> findFollowersWithCursor(UUID followeeId, Instant cursor, UUID idAfter, int limit, String nameLike);
}

package com.team1.otvoo.follow.repository;

import com.team1.otvoo.follow.entity.Follow;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FollowRepositoryCustom {
  List<Follow> findFollowingsWithCursor(UUID followerId, Instant cursor, UUID idAfter, int limit, String nameLike);
  List<Follow> findFollowersWithCursor(UUID followeeId, Instant cursor, UUID idAfter, int limit, String nameLike);
}

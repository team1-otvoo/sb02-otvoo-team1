package com.team1.otvoo.follow.service;

import com.team1.otvoo.follow.dto.FollowCreateRequest;
import com.team1.otvoo.follow.dto.FollowDto;
import com.team1.otvoo.follow.dto.FollowListResponse;
import java.util.UUID;

public interface FollowService {
  FollowDto create(FollowCreateRequest request);
  FollowListResponse getFollowingList(UUID followerId, String cursor, UUID idAfter, int limit, String nameLike);
  FollowListResponse getFollowerList(UUID followerId, String cursor, UUID idAfter, int limit, String nameLike);
  void delete(UUID followId);
}

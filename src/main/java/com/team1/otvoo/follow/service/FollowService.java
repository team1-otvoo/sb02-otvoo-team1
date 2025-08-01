package com.team1.otvoo.follow.service;

import com.team1.otvoo.follow.dto.FollowCreateRequest;
import com.team1.otvoo.follow.dto.FollowDto;
import java.util.UUID;

public interface FollowService {
  FollowDto create(FollowCreateRequest request);
  void delete(UUID followId);
}

package com.team1.otvoo.follow.service.impl;

import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.follow.dto.FollowCreateRequest;
import com.team1.otvoo.follow.dto.FollowDto;
import com.team1.otvoo.follow.entity.Follow;
import com.team1.otvoo.follow.mapper.FollowMapper;
import com.team1.otvoo.follow.repository.FollowRepository;
import com.team1.otvoo.follow.service.FollowService;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.UserRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

  private final FollowRepository followRepository;
  private final UserRepository userRepository;
  private final FollowMapper followMapper;

  @Transactional
  @Override
  public FollowDto create(FollowCreateRequest request) {
    UUID followeeId = request.followeeId();
    UUID followerId = request.followerId();

    if (followeeId.equals(followerId)) {
      throw new RestException(ErrorCode.INVALID_INPUT_VALUE, Map.of("id", followerId, "message", "자기 자신은 팔로우할 수 없습니다."));
    }

    if (followRepository.existsByFolloweeIdAndFollowerId(followeeId, followerId)) {
      throw new RestException(ErrorCode.CONFLICT, Map.of("followeeId", followeeId, "followerId", followerId));
    }

    // findAllById(List<UUID>) 고려
    User followee = userRepository.findById(followeeId)
        .orElseThrow(() -> new RestException(ErrorCode.NOT_FOUND, Map.of("id", followeeId)));
    User follower = userRepository.findById(followerId)
        .orElseThrow(() -> new RestException(ErrorCode.NOT_FOUND, Map.of("id", followerId)));

    Follow follow = new Follow(followee, follower);
    Follow createdFollow = followRepository.save(follow);

    followee.increaseFollowerCount();
    follower.increaseFollowingCount();

    return followMapper.toDto(createdFollow);
  }

  @Transactional
  @Override
  public void delete(UUID followId) {
    // 삭제 대상 유저가 없는 경우?
    Follow follow = followRepository.findById(followId)
        .orElseThrow(() -> new RestException(ErrorCode.NOT_FOUND, Map.of("id", followId)));

    User followee = follow.getFollowee();
    User follower = follow.getFollower();

    followee.decreaseFollowerCount();
    follower.decreaseFollowingCount();

    followRepository.delete(follow);
  }
}

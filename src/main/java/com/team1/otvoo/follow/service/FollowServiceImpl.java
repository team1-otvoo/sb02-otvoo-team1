package com.team1.otvoo.follow.service;

import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.follow.dto.FollowCreateRequest;
import com.team1.otvoo.follow.dto.FollowCursorDto;
import com.team1.otvoo.follow.dto.FollowDto;
import com.team1.otvoo.follow.dto.FollowListResponse;
import com.team1.otvoo.follow.dto.FollowSummaryDto;
import com.team1.otvoo.follow.entity.Follow;
import com.team1.otvoo.follow.mapper.FollowMapper;
import com.team1.otvoo.follow.repository.FollowRepository;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.UserRepository;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

  private final FollowRepository followRepository;
  private final UserRepository userRepository;
  private final FollowMapper followMapper;
  private final ApplicationEventPublisher eventPublisher;

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

    //eventPublisher.publishEvent(new FollowEvent(follower, followee));

    return followMapper.toDto(createdFollow);
  }

  @Transactional(readOnly = true)
  @Override
  public FollowSummaryDto getSummary(UUID userId, UUID myId) {
    User followee = userRepository.findById(userId)
        .orElseThrow(() -> new RestException(ErrorCode.NOT_FOUND, Map.of("id", userId)));

    boolean followedByMe = false;
    UUID followedByMeId = null;
    boolean followingMe = false;

    // 본인이 아닌 경우에만 팔로우 관계 확인
    if (!myId.equals(userId)) {
      // 내가 대상 사용자를 팔로우하는지 확인
      Optional<Follow> followOptional = followRepository.findByFolloweeIdAndFollowerId(userId, myId);
      if (followOptional.isPresent()) {
        followedByMe = true;
        followedByMeId = followOptional.get().getId();
      }

      // 대상 사용자가 나를 팔로우하는지 확인
      followingMe = followRepository.existsByFolloweeIdAndFollowerId(myId, userId);
    }

    return new FollowSummaryDto(
        userId,
        followee.getFollowerCount(),
        followee.getFollowingCount(),
        followedByMe,
        followedByMeId,
        followingMe
    );
  }

  @Transactional(readOnly = true)
  @Override
  public FollowListResponse getFollowingList(UUID followerId, String cursor, UUID idAfter,
      int limit, String nameLike) {

    Instant cursorInstant = null;
    if (cursor != null) {
      try {
        cursorInstant = Instant.parse(cursor);
      } catch (DateTimeParseException e) {
        throw new IllegalArgumentException("Invalid cursor format. Please use ISO-8601 format.", e);
      }
    }

    // hasNext 판별 위해 limit+1 개로 조회
    List<FollowCursorDto> followees = followRepository.findFollowingsWithCursor(followerId, cursorInstant, idAfter, limit+1, nameLike);

    boolean hasNext = followees.size() > limit;
    List<FollowCursorDto> followingList = hasNext ? followees.subList(0, limit) : followees;

    // 조회 결과가 없을 때
    if (followingList.isEmpty()) {
      return new FollowListResponse(
          Collections.emptyList(),
          null,
          null,
          false,
          0L
      );
    }

    long totalCount = followRepository.countByFollowerId(followerId);

    FollowCursorDto last = followingList.get(followingList.size() - 1);
    String nextCursor = last.createdAt().toString();
    UUID nextIdAfter = last.id();

    List<FollowDto> data = followingList.stream()
        .map(fc -> new FollowDto(fc.id(), fc.followee(), fc.follower()))
        .toList();

    return new FollowListResponse(
        data,
        nextCursor,
        nextIdAfter,
        hasNext,
        totalCount
    );
  }

  @Transactional(readOnly = true)
  @Override
  public FollowListResponse getFollowerList(UUID followeeId, String cursor, UUID idAfter, int limit,
      String nameLike) {

    Instant cursorInstant = null;
    if (cursor != null) {
      try {
        cursorInstant = Instant.parse(cursor);
      } catch (DateTimeParseException e) {
        throw new IllegalArgumentException("Invalid cursor format. Please use ISO-8601 format.", e);
      }
    }

    // hasNext 판별 위해 limit+1 개로 조회
    List<FollowCursorDto> followers = followRepository.findFollowersWithCursor(followeeId, cursorInstant, idAfter, limit+1, nameLike);

    boolean hasNext = followers.size() > limit;
    List<FollowCursorDto> followerList = hasNext ? followers.subList(0, limit) : followers;

    // 조회 결과가 없을 때
    if (followerList.isEmpty()) {
      return new FollowListResponse(
          Collections.emptyList(),
          null,
          null,
          false,
          0L
      );
    }

    long totalCount = followRepository.countByFolloweeId(followeeId);

    FollowCursorDto last = followerList.get(followerList.size() - 1);
    String nextCursor = last.createdAt().toString();
    UUID nextIdAfter = last.id();

    List<FollowDto> data = followerList.stream()
        .map(fc -> new FollowDto(fc.id(), fc.followee(), fc.follower()))
        .toList();

    return new FollowListResponse(
        data,
        nextCursor,
        nextIdAfter,
        hasNext,
        totalCount
    );
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

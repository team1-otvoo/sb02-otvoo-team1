package com.team1.otvoo.follow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;

import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.follow.dto.FollowCreateRequest;
import com.team1.otvoo.follow.dto.FollowDto;
import com.team1.otvoo.follow.entity.Follow;
import com.team1.otvoo.follow.mapper.FollowMapper;
import com.team1.otvoo.follow.repository.FollowRepository;
import com.team1.otvoo.follow.service.impl.FollowServiceImpl;
import com.team1.otvoo.user.dto.UserSummary;
import com.team1.otvoo.user.entity.Profile;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class FollowServiceTest {

  @InjectMocks
  private FollowServiceImpl followService;

  @Mock
  private FollowRepository followRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private FollowMapper followMapper;

  private UUID followeeId;
  private UUID followerId;
  private UUID followId;
  private User followee;
  private User follower;
  private Follow follow;
  private FollowDto followDto;

  @BeforeEach
  void setUp() {
    followeeId = UUID.randomUUID();
    followerId = UUID.randomUUID();
    followId = UUID.randomUUID();

    // followee
    Profile followeeProfile = mock(Profile.class);
    followee = spy(new User("test@followee.com", "password123", followeeProfile));
    ReflectionTestUtils.setField(followee, "id", followeeId);

    // follower
    Profile followerProfile = mock(Profile.class);
    follower = spy(new User("test@follower.com", "password123", followerProfile));
    ReflectionTestUtils.setField(follower, "id", followerId);

    // follow entity
    follow = new Follow(followee, follower);
    ReflectionTestUtils.setField(follow, "id", followId);

    // dto
    followDto = new FollowDto(
        followId,
        new UserSummary(followeeId, "followeeName", "http://image.com/followee.jpg"),
        new UserSummary(followerId, "followerName", "http://image.com/follower.jpg")
    );
  }

  @Test
  @DisplayName("팔로우 생성_성공")
  void createFollow_Success_shouldReturnCreatedFollow() {
    // given
    FollowCreateRequest request = new FollowCreateRequest(followeeId, followerId);

    given(followRepository.existsByFolloweeIdAndFollowerId(followeeId, followerId)).willReturn(false);
    given(userRepository.findById(followeeId)).willReturn(Optional.of(followee));
    given(userRepository.findById(followerId)).willReturn(Optional.of(follower));
    given(followRepository.save(any(Follow.class))).willReturn(follow);
    given(followMapper.toDto(follow)).willReturn(followDto);

    // when
    FollowDto result = followService.create(request);

    // then
    assertThat(result.followee().userId()).isEqualTo(request.followeeId());
    assertThat(result.follower().userId()).isEqualTo(request.followerId());

    then(followee).should().increaseFollowerCount();
    then(follower).should().increaseFollowingCount();

    then(followRepository).should().existsByFolloweeIdAndFollowerId(followeeId, followerId);
    then(userRepository).should().findById(followeeId);
    then(userRepository).should().findById(followerId);
    then(followRepository).should().save(any(Follow.class));
    then(followMapper).should().toDto(follow);
  }

  @Test
  @DisplayName("팔로우 생성_실패_자기 자신 팔로우")
  void createFollow_Failure_shouldThrowException_whenFollowingMyself() {
    // given
    UUID sameUserId = UUID.randomUUID();
    FollowCreateRequest request = new FollowCreateRequest(sameUserId, sameUserId);

    // when & then
    RestException exception = assertThrows(RestException.class,
        () -> followService.create(request));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    assertThat(exception.getDetails()).containsEntry("id", sameUserId);
    assertThat(exception.getDetails()).containsEntry("message", "자기 자신은 팔로우할 수 없습니다.");

    then(followRepository).shouldHaveNoInteractions();
    then(userRepository).shouldHaveNoInteractions();
    then(followMapper).should(never()).toDto(follow);
  }

  @Test
  @DisplayName("팔로우 생성_실패_이미 존재하는 팔로우 내역")
  void createFollow_Failure_shouldThrowException_whenDuplicateExists() {
    // given
    FollowCreateRequest request = new FollowCreateRequest(followeeId, followerId);
    given(followRepository.existsByFolloweeIdAndFollowerId(followeeId, followerId)).willReturn(true);

    // when & then
    RestException exception = assertThrows(RestException.class,
        () -> followService.create(request));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
    assertThat(exception.getDetails())
        .containsEntry("followeeId", followeeId)
        .containsEntry("followerId", followerId);

    then(followRepository).should().existsByFolloweeIdAndFollowerId(followeeId, followerId);
    then(userRepository).shouldHaveNoInteractions();
    then(followMapper).should(never()).toDto(follow);
  }

  @Test
  @DisplayName("팔로우 생성_실패_존재하지 않는 팔로우 대상")
  void createFollow_Failure_shouldThrowException_whenFolloweeNotFound() {
    // given
    FollowCreateRequest request = new FollowCreateRequest(followeeId, followerId);

    given(followRepository.existsByFolloweeIdAndFollowerId(followeeId, followerId)).willReturn(false);
    given(userRepository.findById(followeeId)).willReturn(Optional.empty());

    // when & then
    RestException exception = assertThrows(RestException.class,
        () -> followService.create(request));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
    assertThat(exception.getDetails().get("id")).isEqualTo(followeeId);

    then(userRepository).should().findById(followeeId);
    then(userRepository).should(never()).findById(followerId);
    then(followRepository).should(never()).save(any(Follow.class));
    then(followMapper).should(never()).toDto(follow);
  }

  @Test
  @DisplayName("팔로우 취소_성공")
  void deleteFollow_Success() {
    // given
    given(followRepository.findById(followId)).willReturn(Optional.of(follow));

    // when
    followService.delete(followId);

    // then
    then(followee).should().decreaseFollowerCount();
    then(follower).should().decreaseFollowingCount();

    then(followRepository).should().findById(followId);
    then(followRepository).should().delete(follow);
  }

  @Test
  @DisplayName("팔로우 취소_실패_존재하지 않는 팔로우 내역")
  void deleteFollow_Failure_shouldThrowException_whenFollowNotFound() {
    // given
    given(followRepository.findById(followId)).willReturn(Optional.empty());

    // when & then
    RestException exception = assertThrows(RestException.class,
        () -> followService.delete(followId));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
    assertThat(exception.getDetails().get("id")).isEqualTo(followId);

    // then
    then(followRepository).should().findById(followId);
    then(followRepository).should(never()).delete(follow);
  }

}

package com.team1.otvoo.follow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;

import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.follow.dto.FollowCreateRequest;
import com.team1.otvoo.follow.dto.FollowCursorDto;
import com.team1.otvoo.follow.dto.FollowDto;
import com.team1.otvoo.follow.dto.FollowListResponse;
import com.team1.otvoo.follow.dto.FollowSummaryDto;
import com.team1.otvoo.follow.entity.Follow;
import com.team1.otvoo.follow.event.FollowEvent;
import com.team1.otvoo.follow.mapper.FollowMapper;
import com.team1.otvoo.follow.repository.FollowRepository;
import com.team1.otvoo.storage.S3ImageStorageAdapter;
import com.team1.otvoo.user.dto.UserSummary;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.UserRepository;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class FollowServiceTest {

  @InjectMocks
  private FollowServiceImpl followService;

  @Mock
  private FollowRepository followRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private FollowMapper followMapper;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Mock
  private S3ImageStorageAdapter s3ImageStorageAdapter;

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
    followee = spy(new User("test@followee.com", "password123"));
    ReflectionTestUtils.setField(followee, "id", followeeId);

    // follower
    follower = spy(new User("test@follower.com", "password123"));
    ReflectionTestUtils.setField(follower, "id", followerId);

    // follow entity
    follow = new Follow(followee, follower);
    ReflectionTestUtils.setField(follow, "id", followId);
    ReflectionTestUtils.setField(follow, "createdAt", Instant.now());

    // dto
    followDto = new FollowDto(
        followId,
        new UserSummary(followeeId, "followeeName", "http://image.com/followee.jpg"),
        new UserSummary(followerId, "followerName", "http://image.com/follower.jpg")
    );

    given(s3ImageStorageAdapter.getPresignedUrl(anyString()))
        .willAnswer(invocation -> invocation.getArgument(0));
  }

  @Nested
  @DisplayName("팔로우 생성 테스트")
  class CreateFollowTests {

    @Test
    @DisplayName("성공")
    void createFollow_Success_ShouldReturnCreatedFollow() {
      // given
      FollowCreateRequest request = new FollowCreateRequest(followeeId, followerId);

      given(followRepository.existsByFolloweeIdAndFollowerId(followeeId, followerId)).willReturn(false);
      given(userRepository.findById(followeeId)).willReturn(Optional.of(followee));
      given(userRepository.findById(followerId)).willReturn(Optional.of(follower));
      given(followRepository.save(any(Follow.class))).willReturn(follow);
      given(followRepository.findFollowDtoById(followId)).willReturn(followDto);

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
      then(followRepository).should().findFollowDtoById(followId);

      ArgumentCaptor<FollowEvent> eventCaptor = ArgumentCaptor.forClass(FollowEvent.class);
      then(eventPublisher).should().publishEvent(eventCaptor.capture());

      FollowEvent publishedEvent = eventCaptor.getValue();
      assertThat(publishedEvent.follower()).isEqualTo(follower);
      assertThat(publishedEvent.followee()).isEqualTo(followee);
    }

    @Test
    @DisplayName("실패_자기 자신 팔로우")
    void createFollow_Failure_ShouldThrowException_WhenFollowingMyself() {
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

      then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("실패_이미 존재하는 팔로우 내역")
    void createFollow_Failure_ShouldThrowException_WhenDuplicateExists() {
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

      then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("실패_존재하지 않는 팔로우 대상")
    void createFollow_Failure_ShouldThrowException_WhenFolloweeNotFound() {
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

      then(eventPublisher).shouldHaveNoInteractions();
    }

  }

  @Nested
  @DisplayName("팔로우 취소 테스트")
  class DeleteFollowTests {

    @Test
    @DisplayName("성공")
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
    @DisplayName("실패_존재하지 않는 팔로우 내역")
    void deleteFollow_Failure_ShouldThrowException_WhenFollowNotFound() {
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

  @Nested
  @DisplayName("팔로우 요약 정보 조회 테스트")
  class getSummary {

    @Test
    @DisplayName("성공_서로 팔로우하는 경우")
    void getSummary_Success_WhenMutualFollow() {
      // given
      UUID myId = UUID.randomUUID();

      ReflectionTestUtils.setField(followee, "followerCount", 100L);
      ReflectionTestUtils.setField(followee, "followingCount", 50L);

      given(userRepository.findById(followeeId)).willReturn(Optional.of(followee));
      given(followRepository.findByFolloweeIdAndFollowerId(followeeId, myId)).willReturn(Optional.of(follow));
      given(followRepository.existsByFolloweeIdAndFollowerId(myId, followeeId)).willReturn(true);

      // when
      FollowSummaryDto response = followService.getSummary(followeeId, myId);

      // then
      assertThat(response.followeeId()).isEqualTo(followeeId);
      assertThat(response.followerCount()).isEqualTo(100L);
      assertThat(response.followingCount()).isEqualTo(50L);
      assertThat(response.followedByMe()).isTrue();
      assertThat(response.followedByMeId()).isEqualTo(followId);
      assertThat(response.followingMe()).isTrue();

      then(userRepository).should().findById(eq(followeeId));
      then(followRepository).should().findByFolloweeIdAndFollowerId(eq(followeeId), eq(myId));
      then(followRepository).should().existsByFolloweeIdAndFollowerId(eq(myId), eq(followeeId));
    }

    @Test
    @DisplayName("성공_내 팔로우 정보 조회하는 경우")
    void getSummary_Success_WhenMyFollowInfo() {
      // given
      UUID followeeId = UUID.randomUUID();
      UUID myId = followeeId;

      ReflectionTestUtils.setField(followee, "followerCount", 200L);
      ReflectionTestUtils.setField(followee, "followingCount", 100L);

      given(userRepository.findById(followeeId)).willReturn(Optional.of(followee));

      // when
      FollowSummaryDto response = followService.getSummary(followeeId, myId);

      // then
      assertThat(response.followeeId()).isEqualTo(followeeId);
      assertThat(response.followerCount()).isEqualTo(200L);
      assertThat(response.followingCount()).isEqualTo(100L);
      assertThat(response.followedByMe()).isFalse();
      assertThat(response.followedByMeId()).isNull();
      assertThat(response.followingMe()).isFalse();

      then(userRepository).should().findById(eq(followeeId));
      then(followRepository).should(never()).findByFolloweeIdAndFollowerId(any(), any());
      then(followRepository).should(never()).existsByFolloweeIdAndFollowerId(any(), any());
    }

    @Test
    @DisplayName("실패_존재하지 않는 사용자")
    void getSummary_Failure_ShouldThrowException_WhenUserNotFound() {
      // given
      UUID myId = UUID.randomUUID();
      given(userRepository.findById(followeeId)).willReturn(Optional.empty());

      // when & then
      RestException exception = assertThrows(RestException.class,
          () -> followService.getSummary(followeeId, myId));

      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
      assertThat(exception.getDetails().get("id")).isEqualTo(followeeId);

      then(userRepository).should().findById(eq(followeeId));
      then(followRepository).should(never()).findByFolloweeIdAndFollowerId(any(), any());
      then(followRepository).should(never()).existsByFolloweeIdAndFollowerId(any(), any());
    }

  }

  @Nested
  @DisplayName("팔로잉 목록 조회 테스트")
  class GetFollowingListTests {

    @Test
    @DisplayName("성공_다음 페이지가 있는 경우")
    void getFollowingList_Success_WhenHasNextPage() {
      // given
      int limit = 20;

      // limit+1 만큼의 Mock 데이터 생성
      List<FollowCursorDto> mockFollowCursorDtos = createMockFollowCursorDtoList(limit + 1);

      given(followRepository.findFollowingsWithCursor(any(UUID.class), any(), any(), eq(limit + 1), any()))
          .willReturn(mockFollowCursorDtos);
      given(followRepository.countByFollowerId(followerId)).willReturn(100L);

      // when
      FollowListResponse response = followService.getFollowingList(followerId, null, null, limit, null);

      // then
      assertThat(response.hasNext()).isTrue(); // hasNext = true
      assertThat(response.data().size()).isEqualTo(limit);
      assertThat(response.totalCount()).isEqualTo(100L);

      // 마지막 데이터(limit-1 인덱스)로 nextCursor, nextIdAfter가 생성되었는지 확인
      FollowCursorDto lastFollowInPage = mockFollowCursorDtos.get(limit - 1);
      assertThat(response.nextCursor()).isEqualTo(lastFollowInPage.createdAt().toString());
      assertThat(response.nextIdAfter()).isEqualTo(lastFollowInPage.id());

      FollowDto firstDto = response.data().get(0);
      FollowCursorDto firstCursorDto = mockFollowCursorDtos.get(0);
      assertThat(firstDto.id()).isEqualTo(firstCursorDto.id());
      assertThat(firstDto.followee()).isEqualTo(firstCursorDto.followee());
      assertThat(firstDto.follower()).isEqualTo(firstCursorDto.follower());

      then(followRepository).should().findFollowingsWithCursor(eq(followerId), isNull(), isNull(), eq(limit + 1), isNull());
      then(followRepository).should().countByFollowerId(eq(followerId));
    }

    @Test
    @DisplayName("성공_마지막 페이지인 경우")
    void getFollowingList_Success_WhenIsLastPage() {
      // given
      int limit = 20;
      int resultSize = 19;

      // resultSize 만큼의 Mock 데이터 생성
      List<FollowCursorDto> mockFollowCursorDtos = createMockFollowCursorDtoList(resultSize);

      given(followRepository.findFollowingsWithCursor(any(UUID.class), any(), any(), eq(limit + 1), any()))
          .willReturn(mockFollowCursorDtos);
      given(followRepository.countByFollowerId(followerId)).willReturn((long) resultSize);

      // when
      FollowListResponse response = followService.getFollowingList(followerId, null, null, limit, null);

      // then
      assertThat(response.hasNext()).isFalse(); // hasNext = false
      assertThat(response.data().size()).isEqualTo(resultSize);
      assertThat(response.totalCount()).isEqualTo(resultSize);

      // 마지막 데이터(limit-1 인덱스)로 nextCursor, nextIdAfter가 생성되었는지 확인
      FollowCursorDto lastFollowInPage = mockFollowCursorDtos.get(resultSize - 1);
      assertThat(response.nextCursor()).isEqualTo(lastFollowInPage.createdAt().toString());
      assertThat(response.nextIdAfter()).isEqualTo(lastFollowInPage.id());

      then(followRepository).should().findFollowingsWithCursor(eq(followerId), isNull(), isNull(), eq(limit + 1), isNull());
      then(followRepository).should().countByFollowerId(eq(followerId));
    }

    @Test
    @DisplayName("성공_결과가 없는 경우")
    void getFollowingList_Success_WhenResultIsEmpty() {
      // given
      int limit = 20;

      given(followRepository.findFollowingsWithCursor(any(UUID.class), any(), any(), eq(limit + 1), any()))
          .willReturn(Collections.emptyList());

      // when
      FollowListResponse response = followService.getFollowingList(followerId, null, null, limit, null);

      // then
      assertThat(response.hasNext()).isFalse();
      assertThat(response.data()).isEmpty();
      assertThat(response.totalCount()).isEqualTo(0L);
      assertThat(response.nextCursor()).isNull();
      assertThat(response.nextIdAfter()).isNull();

      then(followRepository).should().findFollowingsWithCursor(eq(followerId), isNull(), isNull(), eq(limit + 1), isNull());
      then(followRepository).should(never()).countByFollowerId(eq(followerId));
    }

    @Test
    @DisplayName("실패_유효하지 않은 cursor 포맷")
    void getFollowingList_Failure_ShouldThrowException_WithInvalidCursorFormat() {
      // given
      String invalidCursor = "이건-날짜가-아닙니다";
      int limit = 20;

      // when & then
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
          () -> followService.getFollowingList(followerId, invalidCursor, null, limit, null));

      assertThat(exception.getMessage()).contains("Invalid cursor format");

      then(followRepository).shouldHaveNoInteractions();
      then(followMapper).shouldHaveNoInteractions();
    }

  }

  @Nested
  @DisplayName("팔로워 목록 조회 테스트")
  class getFollowerListTests {

    @Test
    @DisplayName("성공_다음 페이지가 있는 경우")
    void getFollowerList_Success_WhenHasNextPage() {
      // given
      int limit = 20;

      // limit+1 만큼의 Mock 데이터 생성
      List<FollowCursorDto> mockFollowCursorDtos = createMockFollowCursorDtoList(limit + 1);

      given(followRepository.findFollowersWithCursor(any(UUID.class), any(), any(), eq(limit + 1), any()))
          .willReturn(mockFollowCursorDtos);
      given(followRepository.countByFolloweeId(followeeId)).willReturn(100L);

      // when
      FollowListResponse response = followService.getFollowerList(followeeId, null, null, limit, null);

      // then
      assertThat(response.hasNext()).isTrue(); // hasNext = true
      assertThat(response.data().size()).isEqualTo(limit);
      assertThat(response.totalCount()).isEqualTo(100L);

      // 마지막 데이터(limit-1 인덱스)로 nextCursor, nextIdAfter가 생성되었는지 확인
      FollowCursorDto lastFollowInPage = mockFollowCursorDtos.get(limit - 1);
      assertThat(response.nextCursor()).isEqualTo(lastFollowInPage.createdAt().toString());
      assertThat(response.nextIdAfter()).isEqualTo(lastFollowInPage.id());

      FollowDto firstDto = response.data().get(0);
      FollowCursorDto firstCursorDto = mockFollowCursorDtos.get(0);
      assertThat(firstDto.id()).isEqualTo(firstCursorDto.id());
      assertThat(firstDto.followee()).isEqualTo(firstCursorDto.followee());
      assertThat(firstDto.follower()).isEqualTo(firstCursorDto.follower());

      then(followRepository).should().findFollowersWithCursor(eq(followeeId), isNull(), isNull(), eq(limit + 1), isNull());
      then(followRepository).should().countByFolloweeId(eq(followeeId));
    }

    @Test
    @DisplayName("성공_마지막 페이지인 경우")
    void getFollowerList_Success_WhenIsLastPage() {
      // given
      int limit = 20;
      int resultSize = 19;

      // resultSize 만큼의 Mock 데이터 생성
      List<FollowCursorDto> mockFollowCursorDtos = createMockFollowCursorDtoList(resultSize);

      given(followRepository.findFollowersWithCursor(any(UUID.class), any(), any(), eq(limit + 1), any()))
          .willReturn(mockFollowCursorDtos);
      given(followRepository.countByFolloweeId(followeeId)).willReturn((long) resultSize);

      // when
      FollowListResponse response = followService.getFollowerList(followeeId, null, null, limit, null);

      // then
      assertThat(response.hasNext()).isFalse(); // hasNext = false
      assertThat(response.data().size()).isEqualTo(resultSize);
      assertThat(response.totalCount()).isEqualTo(resultSize);

      // 마지막 데이터(limit-1 인덱스)로 nextCursor, nextIdAfter가 생성되었는지 확인
      FollowCursorDto lastFollowInPage = mockFollowCursorDtos.get(resultSize - 1);
      assertThat(response.nextCursor()).isEqualTo(lastFollowInPage.createdAt().toString());
      assertThat(response.nextIdAfter()).isEqualTo(lastFollowInPage.id());

      then(followRepository).should().findFollowersWithCursor(eq(followeeId), isNull(), isNull(), eq(limit + 1), isNull());
      then(followRepository).should().countByFolloweeId(eq(followeeId));
    }

    @Test
    @DisplayName("성공_결과가 없는 경우")
    void getFollowerList_Success_WhenResultIsEmpty() {
      // given
      int limit = 20;

      given(followRepository.findFollowersWithCursor(any(UUID.class), any(), any(), eq(limit + 1), any()))
          .willReturn(Collections.emptyList());

      // when
      FollowListResponse response = followService.getFollowerList(followeeId, null, null, limit, null);

      // then
      assertThat(response.hasNext()).isFalse();
      assertThat(response.data()).isEmpty();
      assertThat(response.totalCount()).isEqualTo(0L);
      assertThat(response.nextCursor()).isNull();
      assertThat(response.nextIdAfter()).isNull();

      then(followRepository).should().findFollowersWithCursor(eq(followeeId), isNull(), isNull(), eq(limit + 1), isNull());
      then(followRepository).should(never()).countByFolloweeId(eq(followeeId));
    }

    @Test
    @DisplayName("실패_유효하지 않은 cursor 포맷")
    void getFollowerList_Failure_ShouldThrowException_WithInvalidCursorFormat() {
      // given
      String invalidCursor = "이건-날짜가-아닙니다";
      int limit = 20;

      // when & then
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
          () -> followService.getFollowerList(followeeId, invalidCursor, null, limit, null));

      assertThat(exception.getMessage()).contains("Invalid cursor format");

      then(followRepository).shouldHaveNoInteractions();
    }

  }

  /**
   * 테스트용 FollowCursorDto 리스트 생성
   */
  private List<FollowCursorDto> createMockFollowCursorDtoList(int count) {
    return IntStream.range(0, count)
        .mapToObj(i -> new FollowCursorDto(
            UUID.randomUUID(),
            Instant.now().minusSeconds(i),
            followDto.followee(),
            followDto.follower()
        ))
        .collect(Collectors.toList());
  }
}

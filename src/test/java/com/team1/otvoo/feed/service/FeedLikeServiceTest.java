package com.team1.otvoo.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.feed.entity.Feed;
import com.team1.otvoo.feed.entity.FeedLike;
import com.team1.otvoo.feed.event.FeedLikeEvent;
import com.team1.otvoo.feed.repository.FeedLikeRepository;
import com.team1.otvoo.feed.repository.FeedRepository;
import com.team1.otvoo.follow.event.FollowEvent;
import com.team1.otvoo.security.CustomUserDetails;
import com.team1.otvoo.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class FeedLikeServiceTest {

  @InjectMocks
  private FeedLikeServiceImpl feedLikeService;
  @Mock
  private FeedLikeRepository feedLikeRepository;
  @Mock
  private FeedRepository feedRepository;
  @Mock
  private SecurityContext securityContext;
  @Mock
  private Authentication authentication;
  @Mock
  private CustomUserDetails customUserDetails;
  @Mock
  private ApplicationEventPublisher eventPublisher;

  private User user;
  private Feed feed;
  private UUID feedId;
  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    user = User.builder()
        .email("test@test.com")
        .build();
    ReflectionTestUtils.setField(user, "id", userId);

    feedId = UUID.randomUUID();
    feed = Feed.builder()
        .content("content")
        .user(user)
        .build();
    ReflectionTestUtils.setField(feed, "id", feedId);
  }

  @Test
  @DisplayName("좋아요 생성 성공")
  void feedLike_create_success() {
    // given
    given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
    SecurityContextHolder.setContext(securityContext);
    given(securityContext.getAuthentication()).willReturn(authentication);
    given(authentication.getPrincipal()).willReturn(customUserDetails);
    given(customUserDetails.getUser()).willReturn(user);
    given(feedLikeRepository.save(any(FeedLike.class))).willAnswer(invocation -> invocation.getArgument(0));

    // when
    feedLikeService.create(feedId);

    // then
    then(feedRepository).should(times(1)).findById(feedId);
    then(feedLikeRepository).should(times(1)).save(any(FeedLike.class));
    then(feedRepository).should(times(1)).incrementLikeCount(feedId);

    ArgumentCaptor<FeedLikeEvent> eventCaptor = ArgumentCaptor.forClass(FeedLikeEvent.class);
    then(eventPublisher).should().publishEvent(eventCaptor.capture());
  }

  @Test
  @DisplayName("좋아요 생성 - 피드가 없으면 예외 발생")
  void feedLike_create_failed_when_feed_notFound() {
    // given
    given(feedRepository.findById(feedId)).willReturn(Optional.empty());

    // then
    assertThatThrownBy(() -> feedLikeService.create(feedId))
        .isInstanceOf(RestException.class)
        .hasMessageContaining(ErrorCode.FEED_NOT_FOUND.getMessage());

    then(feedLikeRepository).should(never()).save(any());
    then(feedRepository).should(never()).incrementLikeCount(any());
    then(eventPublisher).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("좋아요 취소 성공")
  void feedLike_delete_success() {
    // given
    given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
    SecurityContextHolder.setContext(securityContext);
    given(securityContext.getAuthentication()).willReturn(authentication);
    given(authentication.getPrincipal()).willReturn(customUserDetails);
    given(customUserDetails.getUser()).willReturn(user);

    // when
    feedLikeService.delete(feedId);

    // then
    then(feedRepository).should(times(1)).findById(feedId);
    then(feedLikeRepository).should(times(1)).deleteByFeed_IdAndLikedBy_Id(feedId, userId);
    then(feedRepository).should(times(1)).decrementLikerCount(feedId);
  }

  @Test
  @DisplayName("좋아요 취소 - 피드가 없으면 예외 발생")
  void feedLike_delete_failed_when_feed_NotFound() {
    // given
    given(feedRepository.findById(feedId)).willReturn(Optional.empty());

    // then
    assertThatThrownBy(() -> feedLikeService.delete(feedId))
        .isInstanceOf(RestException.class)
        .hasMessageContaining(ErrorCode.FEED_NOT_FOUND.getMessage());

    then(feedLikeRepository).should(never()).deleteByFeed_IdAndLikedBy_Id(any(), any());
    then(feedRepository).should(never()).decrementLikerCount(any());
  }
}
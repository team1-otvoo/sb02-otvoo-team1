package com.team1.otvoo.feed.elasticsearch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;

import com.team1.otvoo.feed.dto.FeedDto;
import com.team1.otvoo.feed.dto.FeedSearchCondition;
import com.team1.otvoo.feed.elasticsearch.repository.FeedSearchRepository;
import com.team1.otvoo.feed.entity.Feed;
import com.team1.otvoo.feed.repository.FeedLikeRepository;
import com.team1.otvoo.feed.repository.FeedRepository;
import com.team1.otvoo.security.CustomUserDetails;
import com.team1.otvoo.user.dto.AuthorDto;
import com.team1.otvoo.user.entity.Profile;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.ProfileRepository;
import com.team1.otvoo.user.resolver.ProfileImageUrlResolver;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FeedElasticSearchServiceTest {
  @Mock
  private FeedSearchRepository feedSearchRepository;
  @Mock
  private FeedRepository feedRepository;
  @Mock
  private FeedLikeRepository feedLikeRepository;
  @Mock
  private ProfileImageUrlResolver profileImageUrlResolver;
  @Mock
  private ProfileRepository profileRepository;

  @InjectMocks
  private FeedElasticSearchService feedElasticSearchService;

  private UUID userId;
  private User user;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    user = new User("test@test.com", "test1234$");
    ReflectionTestUtils.setField(user, "id", userId);

    CustomUserDetails principal = new CustomUserDetails(user);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null)
    );
  }

  @Test
  @DisplayName("피드 Elasticsearch 조회 성공")
  void getFeedsWithCursor_success() {
    // given
    UUID feedId = UUID.randomUUID();
    UUID authorUserId = UUID.randomUUID();
    UUID profileId = UUID.randomUUID();

    FeedDto dto = FeedDto.builder()
        .id(feedId)
        .author(new AuthorDto(authorUserId, "author", null))
        .build();

    Slice<FeedDto> slice = new SliceImpl<>(List.of(dto));

    given(feedSearchRepository.searchFeeds(any(FeedSearchCondition.class))).willReturn(slice);

    Feed feed = Feed.builder()
        .build();
    ReflectionTestUtils.setField(feed, "id", feedId);
    ReflectionTestUtils.setField(feed, "commentCount", 5L);

    Profile profile = new Profile("profile", user);
    ReflectionTestUtils.setField(profile, "id", profileId);
    given(profileRepository.findByUserId(authorUserId)).willReturn(Optional.of(profile));

    given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
    given(feedLikeRepository.existsFeedLikeByFeed_IdAndLikedBy_Id(feedId, userId)).willReturn(true);
    given(profileRepository.findByUserId(authorUserId))
        .willReturn(Optional.of(profile));
    given(profileImageUrlResolver.resolve(profileId)).willReturn("http://image-url");

    // when
    Slice<FeedDto> result = feedElasticSearchService.getFeedsWithCursor(FeedSearchCondition.builder()
        .build());

    // then
    FeedDto feedDto = result.getContent().get(0);
    assertThat(feedDto.getCommentCount()).isEqualTo(5L);
    assertThat(feedDto.isLikedByMe()).isTrue();
    assertThat(feedDto.getAuthor().getProfileImageUrl()).isEqualTo("http://image-url");

    then(feedRepository).should(times(1)).findById(feedId);
    then(feedLikeRepository).should(times(1))
        .existsFeedLikeByFeed_IdAndLikedBy_Id(feedId, userId);
    then(profileRepository).should(times(1)).findByUserId(authorUserId);
    then(profileImageUrlResolver).should(times(1)).resolve(profileId);
  }
}

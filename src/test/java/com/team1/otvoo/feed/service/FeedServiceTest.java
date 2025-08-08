package com.team1.otvoo.feed.service;

import com.team1.otvoo.clothes.dto.OotdDto;
import com.team1.otvoo.clothes.entity.Clothes;
import com.team1.otvoo.clothes.mapper.ClothesMapper;
import com.team1.otvoo.clothes.repository.ClothesRepository;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.feed.dto.FeedCreateRequest;
import com.team1.otvoo.feed.dto.FeedDto;
import com.team1.otvoo.feed.dto.FeedSearchCondition;
import com.team1.otvoo.feed.dto.FeedUpdateRequest;
import com.team1.otvoo.feed.entity.Feed;
import com.team1.otvoo.feed.entity.FeedClothes;
import com.team1.otvoo.feed.mapper.FeedMapper;
import com.team1.otvoo.feed.repository.FeedClothesRepository;
import com.team1.otvoo.feed.repository.FeedLikeRepository;
import com.team1.otvoo.feed.repository.FeedRepository;
import com.team1.otvoo.security.CustomUserDetails;
import com.team1.otvoo.user.entity.Profile;
import com.team1.otvoo.user.entity.ProfileImage;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.ProfileImageRepository;
import com.team1.otvoo.user.repository.ProfileRepository;
import com.team1.otvoo.user.repository.UserRepository;
import com.team1.otvoo.weather.repository.WeatherForecastRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class FeedServiceTest {

  @Mock
  UserRepository userRepository;
  @Mock
  WeatherForecastRepository weatherForecastRepository;
  @Mock
  ClothesRepository clothesRepository;
  @Mock
  ProfileRepository profileRepository;
  @Mock
  ProfileImageRepository profileImageRepository;
  @Mock
  FeedClothesRepository feedClothesRepository;
  @Mock
  FeedRepository feedRepository;
  @Mock
  ClothesMapper clothesMapper;
  @Mock
  FeedLikeRepository feedLikeRepository;
  @Mock
  FeedMapper feedMapper;
  @InjectMocks
  FeedServiceImpl feedService;

  UUID userId = UUID.randomUUID();
  UUID weatherForecastId = UUID.randomUUID();
  UUID clothesId = UUID.randomUUID();
  FeedCreateRequest request = new FeedCreateRequest(userId, weatherForecastId, List.of(clothesId),
      "testContent");

  @Test
  @DisplayName("피드 생성 성공")
  void feed_create_success() {
    // given
    User user = User.builder()
        .email("test@test.com")
        .build();
    ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

    Profile profile = new Profile("testProfile", user);
    ReflectionTestUtils.setField(profile, "id", UUID.randomUUID());
    ProfileImage profileImage = new ProfileImage("test.url", "test", "jpeg", 0L, 1, 1, profile);

    given(userRepository.findById(any(UUID.class))).willReturn(Optional.of(user));
    given(profileRepository.findByUserId(any(UUID.class))).willReturn(Optional.of(profile));
    given(profileImageRepository.findByProfileId(any(UUID.class))).willReturn(
        Optional.of(profileImage));
    given(weatherForecastRepository.findById(any(UUID.class))).willReturn(mock());
    given(clothesRepository.findAllById(any())).willReturn(List.of());
    given(feedMapper.toDto(any(Feed.class), any(), anyBoolean())).willAnswer(invocation -> {
      Feed feedArg = invocation.getArgument(0);
      return FeedDto.builder()
          .content(feedArg.getContent())
          .build();
    });

    // when
    FeedDto createdFeed = feedService.create(request);

    // then
    assertThat(createdFeed.getContent()).isEqualTo("testContent");
    then(feedRepository).should(times(1)).save(any());
  }

  @Test
  @DisplayName("피드 생성 실패 - 유저가 존재하지 않을 때")
  void feed_create_failed_when_user_not_found() {
    // given
    given(userRepository.findById(any(UUID.class))).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> feedService.create(request))
        .isInstanceOf(RestException.class)
        .hasMessageContaining("유저가 존재하지 않습니다.");
    then(feedRepository).should(never()).save(any());
  }

  @Test
  @DisplayName("피드 생성 실패 - 날씨 데이터가 존재하지 않을 때")
  void feed_create_failed_when_weatherForecast_not_found() {
    // given
    given(userRepository.findById(any(UUID.class))).willReturn(mock());
    given(weatherForecastRepository.findById(any(UUID.class))).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> feedService.create(request))
        .isInstanceOf(RestException.class)
        .hasMessageContaining("날씨 데이터가 존재하지 않습니다.");
    then(feedRepository).should(never()).save(any());
  }

  @Test
  @DisplayName("피드 수정 성공")
  void feed_update_success() {
    // given
    User user = User.builder()
        .email("test@test.com")
        .build();
    ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
    Profile profile = new Profile("testProfile", user);
    ReflectionTestUtils.setField(profile, "id", UUID.randomUUID());
    ProfileImage profileImage = new ProfileImage("test.url", "test", "jpeg", 0L, 1, 1, profile);

    UUID feedId = UUID.randomUUID();
    Feed feed = Feed.builder()
        .content("test")
        .user(user)
        .build();
    ReflectionTestUtils.setField(feed, "id", feedId);

    Authentication authentication = mock(Authentication.class);
    CustomUserDetails customUserDetails = mock(CustomUserDetails.class);

    given(profileRepository.findByUserId(any(UUID.class))).willReturn(Optional.of(profile));
    given(profileImageRepository.findByProfileId(any(UUID.class))).willReturn(
        Optional.of(profileImage));
    given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
    given(authentication.getPrincipal()).willReturn(customUserDetails);
    given(customUserDetails.getUser()).willReturn(user);
    given(feedLikeRepository.existsFeedLikeByFeed_IdAndLikedBy_Id(any(),any())).willReturn(true);
    given(feedMapper.toDto(any(Feed.class), any(), anyBoolean())).willAnswer(invocation -> {
      Feed feedArg = invocation.getArgument(0);
      return FeedDto.builder()
          .content(feedArg.getContent())
          .build();
    });
    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(authentication);
    SecurityContextHolder.setContext(securityContext);

    FeedUpdateRequest request = new FeedUpdateRequest("update test");

    // when
    FeedDto updatedFeed = feedService.update(feedId, request);

    // then
    assertThat(updatedFeed.getContent()).isEqualTo("update test");
    then(feedRepository).should(times(1)).save(feed);
  }

  @Test
  @DisplayName("피드 수정 실패 - 피드가 존재하지 않을 때")
  void feed_update_failed_when_feed_not_found() {
    // given
    UUID feedId = UUID.randomUUID();
    FeedUpdateRequest request = new FeedUpdateRequest("update test");
    given(feedRepository.findById(feedId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> feedService.update(feedId, request))
        .isInstanceOf(RestException.class)
        .hasMessageContaining("피드가 존재하지 않습니다.");
    then(feedRepository).should(never()).save(any());
  }

  @Test
  @DisplayName("피드 삭제 성공")
  void feed_delete_success() {
    // given
    UUID feedId = UUID.randomUUID();
    Feed feed = Feed.builder()
        .content("test")
        .build();
    ReflectionTestUtils.setField(feed, "id", feedId);

    given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));

    // when
    feedService.delete(feedId);

    // then
    then(feedRepository).should(times(1)).delete(feed);
  }

  @Test
  @DisplayName("피드 삭제 실패 - 피드가 존재하지 않을 때")
  void feed_delete_failed_when_feed_not_found() {
    // given
    UUID feedId = UUID.randomUUID();
    given(feedRepository.findById(feedId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> feedService.delete(feedId))
        .isInstanceOf(RestException.class)
        .hasMessageContaining("피드가 존재하지 않습니다.");
    then(feedRepository).should(never()).delete(any());
  }

  @Test
  @DisplayName("피드 목록 조회 성공")
  void get_feeds_success() {
    // given
    UUID feedId = UUID.randomUUID();

    FeedDto feedDto = FeedDto.builder()
        .id(feedId)
        .content("test")
        .build();

    User user = User.builder()
        .email("test@test.com")
        .password("test1234!")
        .build();
    ReflectionTestUtils.setField(user,"id", UUID.randomUUID());

    Slice<FeedDto> feedSlice = new SliceImpl<>(List.of(feedDto));
    given(feedRepository.searchByCondition(any(),any())).willReturn(feedSlice);

    Authentication authentication = mock(Authentication.class);
    CustomUserDetails customUserDetails = mock(CustomUserDetails.class);
    given(authentication.getPrincipal()).willReturn(customUserDetails);
    given(customUserDetails.getUser()).willReturn(user);
    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(authentication);
    SecurityContextHolder.setContext(securityContext);

    FeedClothes feedClothes = mock(FeedClothes.class);
    Feed feed = mock(Feed.class);
    Clothes clothes = mock(Clothes.class);
    given(feed.getId()).willReturn(feedId);
    given(feedClothes.getFeed()).willReturn(feed);
    given(feedClothes.getClothes()).willReturn(clothes);

    given(feedClothesRepository.findAllByFeedIdInWithClothesAndSelectedValues(List.of(feedId)))
        .willReturn(List.of(feedClothes));

    OotdDto ootdDto = mock(OotdDto.class);
    given(clothesMapper.toOotdDto(clothes)).willReturn(ootdDto);

    // when
    Slice<FeedDto> result = feedService.getFeedsWithCursor(mock(FeedSearchCondition.class));

    // then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getContent()).isEqualTo("test");
    assertThat(result.getContent().get(0).getOotds()).containsExactly(ootdDto);
  }
}
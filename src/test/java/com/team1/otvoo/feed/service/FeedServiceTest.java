package com.team1.otvoo.feed.service;

import com.team1.otvoo.clothes.repository.ClothesRepository;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.feed.dto.FeedCreateRequest;
import com.team1.otvoo.feed.dto.FeedDto;
import com.team1.otvoo.feed.dto.FeedUpdateRequest;
import com.team1.otvoo.feed.entity.Feed;
import com.team1.otvoo.feed.mapper.FeedMapper;
import com.team1.otvoo.feed.repository.FeedRepository;
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
  FeedRepository feedRepository;
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
    given(userRepository.findById(any(UUID.class))).willReturn(mock());
    given(weatherForecastRepository.findById(any(UUID.class))).willReturn(mock());
    given(clothesRepository.findAllById(any())).willReturn(List.of());

    given(feedMapper.toDto(any(Feed.class), anyBoolean())).willAnswer(invocation -> {
      Feed feedArg = invocation.getArgument(0);
      return FeedDto.builder()
          .content(feedArg.getContent())
          .build();
    });

    // when
    FeedDto createdFeed = feedService.create(request);

    // then
    assertThat(createdFeed.content()).isEqualTo("testContent");
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
        .hasMessageContaining("찾을 수 없습니다.");
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
        .hasMessageContaining("찾을 수 없습니다.");
    then(feedRepository).should(never()).save(any());
  }

  @Test
  @DisplayName("피드 수정 성공")
  void feed_update_success() {
    // given
    UUID feedId = UUID.randomUUID();
    Feed feed = Feed.builder()
        .content("test")
        .build();
    ReflectionTestUtils.setField(feed, "id", feedId);

    given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
    given(feedMapper.toDto(any(Feed.class), anyBoolean())).willAnswer(invocation -> {
      Feed feedArg = invocation.getArgument(0);
      return FeedDto.builder()
          .content(feedArg.getContent())
          .build();
    });

    FeedUpdateRequest request = new FeedUpdateRequest("update test");

    // when
    FeedDto updatedFeed = feedService.update(feedId, request);

    // then
    assertThat(updatedFeed.content()).isEqualTo("update test");
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
        .hasMessageContaining("찾을 수 없습니다.");
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
        .hasMessageContaining("찾을 수 없습니다.");
    then(feedRepository).should(never()).delete(any());
  }
}
package com.team1.otvoo.feed.service;

import com.team1.otvoo.clothes.entity.Clothes;
import com.team1.otvoo.clothes.repository.ClothesRepository;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.feed.dto.FeedCreateRequest;
import com.team1.otvoo.feed.dto.FeedDto;
import com.team1.otvoo.feed.dto.FeedSearchCondition;
import com.team1.otvoo.feed.dto.FeedUpdateRequest;
import com.team1.otvoo.feed.entity.Feed;
import com.team1.otvoo.feed.entity.FeedClothes;
import com.team1.otvoo.feed.mapper.FeedMapper;
import com.team1.otvoo.feed.repository.FeedRepository;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.UserRepository;
import com.team1.otvoo.weather.entity.WeatherForecast;
import com.team1.otvoo.weather.repository.WeatherForecastRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedServiceImpl implements FeedService {

  private final UserRepository userRepository;
  private final WeatherForecastRepository weatherForecastRepository;
  private final ClothesRepository clothesRepository;
  private final FeedRepository feedRepository;
  private final FeedMapper feedMapper;

  @Transactional
  @Override
  public FeedDto create(FeedCreateRequest request) {
    User user = userRepository.findById(request.authorId()).orElseThrow(
        () -> {
          log.warn("해당 유저가 존재하지 않습니다. - userId: {}", request.authorId());
          return new RestException(ErrorCode.NOT_FOUND,
              Map.of("authorId", request.authorId(), "detail", "User not found"));
        });

    WeatherForecast weather = weatherForecastRepository.findById(request.weatherId()).orElseThrow(
        () -> {
          log.warn("해당 날씨 데이터가 존재하지 않습니다. - weatherId: {}", request.weatherId());
          return new RestException(ErrorCode.NOT_FOUND,
          Map.of("weatherId", request.weatherId(),"detail", "WeatherForecast not found"));
        });

    List<Clothes> clothesList = clothesRepository.findAllById(request.clothesIds());

    Feed createdFeed = Feed.builder()
        .content(request.content())
        .user(user)
        .weatherForecast(weather)
        .build();

    List<FeedClothes> feedClothesList = clothesList.stream()
        .map(clothes -> new FeedClothes(createdFeed, clothes))
        .toList();

    createdFeed.updateFeedClothes(feedClothesList);
    feedRepository.save(createdFeed);

    return feedMapper.toDto(createdFeed, false);
  }

  @Transactional
  @Override
  public FeedDto update(UUID id, FeedUpdateRequest request) {
    Feed feed = findFeed(id);
    feed.updateFeed(request.content());

    feedRepository.save(feed);

    return feedMapper.toDto(feed, false); // likedByMe는 like 구현 후 수정 예정
  }

  @Transactional(readOnly = true)
  @Override
  public Slice<FeedDto> getFeedsWithCursor(FeedSearchCondition searchCondition) {
    Slice<Feed> feeds = feedRepository.searchByCondition(searchCondition);
    // 좋아요 구현 후 반영 예정
    return feeds.map(feed -> feedMapper.toDto(feed, false));
  }

  @Transactional
  @Override
  public void delete(UUID id) {
    Feed feed = findFeed(id);

    feedRepository.delete(feed);
  }

  private Feed findFeed(UUID id) {
    return feedRepository.findById(id)
        .orElseThrow(() -> {
          log.warn("해당 피드가 존재하지 않습니다. - feedId: {}", id);
          return new RestException(ErrorCode.NOT_FOUND,
              Map.of("feedId", id, "detail", "Feed not found"));
        });
  }
}
package com.team1.otvoo.feed.service;

import com.team1.otvoo.clothes.dto.OotdDto;
import com.team1.otvoo.clothes.entity.Clothes;
import com.team1.otvoo.clothes.entity.ClothesImage;
import com.team1.otvoo.clothes.mapper.ClothesMapper;
import com.team1.otvoo.clothes.repository.ClothesImageRepository;
import com.team1.otvoo.clothes.repository.ClothesRepository;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.feed.dto.FeedCreateRequest;
import com.team1.otvoo.feed.dto.FeedDto;
import com.team1.otvoo.feed.dto.FeedSearchCondition;
import com.team1.otvoo.feed.dto.FeedUpdateRequest;
import com.team1.otvoo.feed.entity.Feed;
import com.team1.otvoo.feed.entity.FeedClothes;
import com.team1.otvoo.feed.event.FeedEvent;
import com.team1.otvoo.feed.mapper.FeedMapper;
import com.team1.otvoo.feed.repository.FeedClothesRepository;
import com.team1.otvoo.feed.repository.FeedLikeRepository;
import com.team1.otvoo.feed.repository.FeedRepository;
import com.team1.otvoo.security.CustomUserDetails;
import com.team1.otvoo.user.dto.AuthorDto;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.UserRepository;
import com.team1.otvoo.weather.entity.WeatherForecast;
import com.team1.otvoo.weather.repository.WeatherForecastRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedServiceImpl implements FeedService {

  private final UserRepository userRepository;
  private final WeatherForecastRepository weatherForecastRepository;
  private final ClothesRepository clothesRepository;
  private final ClothesImageRepository clothesImageRepository;
  private final FeedRepository feedRepository;
  private final FeedLikeRepository feedLikeRepository;
  private final FeedClothesRepository feedClothesRepository;
  private final FeedMapper feedMapper;
  private final ClothesMapper clothesMapper;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  @Override
  public FeedDto create(FeedCreateRequest request) {
    User user = findUser(request.authorId());
    WeatherForecast weather = findForecast(request.weatherId());
    AuthorDto authorDto = userRepository.projectionAuthorDtoById(user.getId());

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
    Feed savedFeed = feedRepository.save(createdFeed);

    eventPublisher.publishEvent(new FeedEvent(savedFeed));

    return feedMapper.toDto(createdFeed, authorDto,false);
  }

  @Transactional
  @Override
  public FeedDto update(UUID id, FeedUpdateRequest request) {
    Feed feed = findFeed(id);
    User user = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUser();
    AuthorDto authorDto = userRepository.projectionAuthorDtoById(user.getId());

    feed.updateFeed(request.content());

    feedRepository.save(feed);

    return feedMapper.toDto(feed, authorDto,
        feedLikeRepository.existsFeedLikeByFeed_IdAndLikedBy_Id(id, user.getId()));
  }

  @Transactional(readOnly = true)
  @Override
  public Slice<FeedDto> getFeedsWithCursor(FeedSearchCondition searchCondition) {
    User user = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUser();
    Slice<FeedDto> feeds = feedRepository.searchByCondition(searchCondition, user.getId());

    List<UUID> feedIds = feeds.stream()
        .map(FeedDto::getId)
        .toList();

    List<FeedClothes> feedClothesList =
        feedClothesRepository.findAllByFeedIdInWithClothesAndSelectedValues(feedIds);

    List<UUID> clothesIds = feedClothesList.stream()
        .map(fc -> fc.getClothes().getId())
        .distinct()
        .toList();

    List<ClothesImage> images = clothesImageRepository.findAllByClothes_IdIn(clothesIds);
    Map<UUID, String> imageMap = images.stream()
        .collect(Collectors.toMap(img -> img.getClothes().getId(), ClothesImage::getImageUrl, (a, b) -> a));

    // feedId로 그룹핑 후, 연관된 Clothes들을 ootdDto List로 변환
    // Collectors.mapping은 groupingBy로 묶인 각 그룹 내부 요소에만 작동
    Map<UUID, List<OotdDto>> ootdMap = feedClothesList.stream()
        .collect(Collectors.groupingBy(
            fc -> fc.getFeed().getId(),
            Collectors.mapping(
                fc -> {
                  Clothes clothes = fc.getClothes();
                  String imageUrl = imageMap.get(clothes.getId());
                  return clothesMapper.toOotdDto(clothes, imageUrl);
                },
                Collectors.toList()
            )
        ));

    // FeedDto에 OotdDto 리스트 세팅
    for (FeedDto feedDto : feeds) {
      feedDto.setOotds(ootdMap.getOrDefault(feedDto.getId(), Collections.emptyList()));
    }

    return feeds;
  }

  @Transactional
  @Override
  public void delete(UUID id) {
    Feed feed = findFeed(id);

    feedRepository.delete(feed);
  }

  private Feed findFeed(UUID id) {
    return feedRepository.findById(id).orElseThrow(
        () -> new RestException(ErrorCode.FEED_NOT_FOUND,
            Map.of("feedId", id)));
  }

  private User findUser(UUID id) {
    return userRepository.findById(id).orElseThrow(
        () ->  new RestException(ErrorCode.USER_NOT_FOUND,
            Map.of("authorId", id)));
  }

  private WeatherForecast findForecast(UUID id) {
    return weatherForecastRepository.findById(id).orElseThrow(
        () -> new RestException(ErrorCode.WEATHER_FORECAST_NOT_FOUND,
              Map.of("weatherId", id)));
  }
}
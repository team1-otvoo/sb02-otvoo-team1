package com.team1.otvoo.feed.mapper;

import com.team1.otvoo.clothes.dto.OotdDto;
import com.team1.otvoo.clothes.mapper.ClothesMapper;
import com.team1.otvoo.feed.dto.FeedDto;
import com.team1.otvoo.feed.entity.Feed;
import com.team1.otvoo.feed.entity.FeedClothes;
import com.team1.otvoo.user.mapper.UserMapper;
import com.team1.otvoo.weather.mapper.WeatherMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring",
    uses = {UserMapper.class, ClothesMapper.class, WeatherMapper.class})
public interface FeedMapper {
  @Mapping(source = "feed.user", target = "author")
  @Mapping(source = "feed.feedClothes", target = "ootds")
  @Mapping(source = "feed.weather", target = "weather")
  @Mapping(source = "likedByMe", target = "likedByMe")
  FeedDto toDto(Feed feed, boolean likedByMe);

  default OotdDto feedClothesToOotdDto(FeedClothes feedClothes) {
    if (feedClothes == null || feedClothes.getClothes() == null) {
      return null;
    }
    return ClothesMapper.INSTANCE.toOotdDto(feedClothes.getClothes());
  }
}

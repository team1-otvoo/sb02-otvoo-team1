package com.team1.otvoo.user.mapper;

import com.team1.otvoo.user.dto.ProfileDto;
import com.team1.otvoo.user.entity.Profile;
import com.team1.otvoo.weather.mapper.WeatherMapper;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = WeatherMapper.class)
public interface ProfileMapper {

  @Mapping(source = "userId", target = "userId")
  @Mapping(source = "profile.birth", target = "birthDate")
  @Mapping(source = "profile.location", target = "location")
  @Mapping(source = "profileImageUrl", target = "profileImageUrl")
  ProfileDto toProfileDto(UUID userId, Profile profile, String profileImageUrl);

}

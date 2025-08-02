package com.team1.otvoo.user.mapper;

import com.team1.otvoo.user.dto.AuthorDto;
import com.team1.otvoo.user.dto.UserDto;
import com.team1.otvoo.user.dto.UserSummary;
import com.team1.otvoo.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

  @Mapping(source = "profile.name", target = "name")
  @Mapping(target = "linkedOAuthProviders", expression = "java(java.util.List.of())")   // OAuth 구현시 변경 필요!!!
  UserDto toUserDto(User user);

  @Mapping(source = "id", target = "userId")
  @Mapping(source = "profile.name", target = "name")
  @Mapping(source = "profile.profileImage.imageUrl", target = "profileImageUrl")
  UserSummary toSummary(User user);

  @Mapping(source = "id", target = "userId")
  @Mapping(source = "profile.name", target = "name")
  @Mapping(source = "profile.profileImage.imageUrl", target = "profileImageUrl")
  AuthorDto toAuthorDto(User user);
}

package com.team1.otvoo.follow.mapper;

import com.team1.otvoo.follow.dto.FollowDto;
import com.team1.otvoo.follow.entity.Follow;
import com.team1.otvoo.user.mapper.UserMapper;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface FollowMapper {
  FollowDto toDto(Follow follow);
  List<FollowDto> toDtoList(List<Follow> follows);
}

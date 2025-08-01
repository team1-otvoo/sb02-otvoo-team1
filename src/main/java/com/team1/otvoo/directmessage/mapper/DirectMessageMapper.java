package com.team1.otvoo.directmessage.mapper;

import com.team1.otvoo.directmessage.dto.DirectMessageDto;
import com.team1.otvoo.directmessage.dto.DirectMessageResponse;
import com.team1.otvoo.directmessage.entity.DirectMessage;
import com.team1.otvoo.user.util.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface DirectMessageMapper {

  @Mapping(target = "senderId", source = "sender.id")
  @Mapping(target = "receiverId", source = "receiver.id")
  DirectMessageResponse toResponse(DirectMessage directMessage);

  @Mapping(target = "sender", source = "sender")
  @Mapping(target = "receiver", source = "receiver")
  DirectMessageDto toDto(DirectMessage directMessage);
}
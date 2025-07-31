package com.team1.otvoo.directmessage.mapper;

import com.team1.otvoo.directmessage.dto.DirectMessageCreateRequest;
import com.team1.otvoo.directmessage.dto.DirectMessageResponse;
import com.team1.otvoo.directmessage.entity.DirectMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DirectMessageMapper {

  @Mapping(target = "senderId", source = "sender.id")
  @Mapping(target = "receiverId", source = "receiver.id")
  DirectMessageResponse toResponse(DirectMessage directMessage);

  DirectMessage toEntity(DirectMessageCreateRequest request);
}
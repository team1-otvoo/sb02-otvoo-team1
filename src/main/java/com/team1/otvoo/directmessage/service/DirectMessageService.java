package com.team1.otvoo.directmessage.service;

import com.team1.otvoo.directmessage.dto.DirectMessageCreateRequest;
import com.team1.otvoo.directmessage.dto.DirectMessageDto;
import com.team1.otvoo.directmessage.dto.DirectMessageDtoCursorResponse;

import java.util.UUID;

public interface DirectMessageService {
  DirectMessageDto createDto(DirectMessageCreateRequest request);
  DirectMessageDtoCursorResponse getDirectMessagesByUserId(UUID userId, String cursorStr, String idAfterStr, int limit);
}
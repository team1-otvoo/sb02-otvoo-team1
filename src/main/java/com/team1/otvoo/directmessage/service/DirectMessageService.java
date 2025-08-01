package com.team1.otvoo.directmessage.service;

import com.team1.otvoo.directmessage.dto.DirectMessageCreateRequest;
import com.team1.otvoo.directmessage.dto.DirectMessageDtoCursorResponse;
import com.team1.otvoo.directmessage.dto.DirectMessageResponse;

import java.util.UUID;

public interface DirectMessageService {
  DirectMessageResponse create(DirectMessageCreateRequest request);
  DirectMessageDtoCursorResponse getDirectMessageByuserId(UUID userId, String cursorStr, String idAfterStr, int limit);
}

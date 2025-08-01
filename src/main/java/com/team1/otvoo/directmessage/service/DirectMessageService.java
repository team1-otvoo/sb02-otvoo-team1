package com.team1.otvoo.directmessage.service;

import com.team1.otvoo.directmessage.dto.DirectMessageCreateRequest;
import com.team1.otvoo.directmessage.dto.DirectMessageResponse;

public interface DirectMessageService {
  DirectMessageResponse create(DirectMessageCreateRequest request);
}

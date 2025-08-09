package com.team1.otvoo.directmessage.repository;

import com.team1.otvoo.directmessage.dto.DirectMessageDto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DirectMessageRepositoryCustom {
  List<DirectMessageDto> findDirectMessagesWithCursor(UUID userId, Instant cursor, UUID idAfter, int limit);
  long countDirectMessagesByUserId(UUID userId);
  DirectMessageDto findByIdWithUserSummaries(UUID id);
}
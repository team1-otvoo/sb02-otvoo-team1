package com.team1.otvoo.directmessage.repository;

import com.team1.otvoo.directmessage.dto.DirectMessageDto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DirectMessageRepositoryCustom {
  DirectMessageDto findByIdWithUserSummaries(UUID id);
  List<DirectMessageDto> findDirectMessagesBetweenUsersWithCursor(UUID userId1, UUID userId2, Instant cursor, UUID idAfter, int limit);
  long countDirectMessagesBetweenUsers(UUID userId1, UUID userId2);
}

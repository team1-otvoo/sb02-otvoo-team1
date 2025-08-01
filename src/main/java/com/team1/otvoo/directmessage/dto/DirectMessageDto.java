package com.team1.otvoo.directmessage.dto;

import com.team1.otvoo.user.dto.UserSummary;

import java.time.Instant;
import java.util.UUID;

public record DirectMessageDto(
    UUID id,
    Instant createdAt,
    UserSummary sender,
    UserSummary receiver,
    String content
) {
}

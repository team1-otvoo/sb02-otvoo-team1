package com.team1.otvoo.directmessage.dto;

import java.time.Instant;
import java.util.UUID;

public record DirectMessageResponse(
    UUID senderId,
    UUID receiverId,
    String content,
    Instant createdAt
) {
}

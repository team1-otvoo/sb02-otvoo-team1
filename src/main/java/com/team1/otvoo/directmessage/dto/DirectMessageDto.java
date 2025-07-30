package com.team1.otvoo.directmessage.dto;

import com.team1.otvoo.user.dto.UserDto;

import java.time.Instant;
import java.util.UUID;

public record DirectMessageDto(
        UUID id,
        Instant createdAt,
        UserDto sender,
        UserDto receiver,
        String content
) {
}

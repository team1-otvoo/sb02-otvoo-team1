package com.team1.otvoo.directmessage.dto;

import java.util.UUID;

public record DirectMessageCreateRequest(
    UUID senderId,
    UUID receiverId,
    String content
) {

}
package com.team1.otvoo.user.dto;

import java.util.UUID;

public record AuthorDto(
    UUID userId,
    String name,
    String profileImageUrl
) {

}

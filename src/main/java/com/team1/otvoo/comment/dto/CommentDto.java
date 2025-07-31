package com.team1.otvoo.comment.dto;

import com.team1.otvoo.user.dto.AuthorDto;
import java.time.Instant;
import java.util.UUID;

public record CommentDto(
    UUID id,
    Instant createdAt,
    UUID feedId,
    AuthorDto author,
    String content
) {

}

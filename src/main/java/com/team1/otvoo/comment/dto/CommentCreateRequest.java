package com.team1.otvoo.comment.dto;

import java.util.UUID;

public record CommentCreateRequest(
    UUID feedId,
    UUID authorId,
    String content
) {

}

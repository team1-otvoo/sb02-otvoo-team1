package com.team1.otvoo.feed.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record FeedCreateRequest(
    UUID authorId,
    UUID weatherId,
    List<UUID> clothesIds,
    @NotEmpty(message = "내용을 입력해주세요.")
    String content
) {

}
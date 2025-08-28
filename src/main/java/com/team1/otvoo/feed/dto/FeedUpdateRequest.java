package com.team1.otvoo.feed.dto;

import jakarta.validation.constraints.NotEmpty;

public record FeedUpdateRequest(
    @NotEmpty(message = "내용을 입력해주세요.")
    String content
) {
}

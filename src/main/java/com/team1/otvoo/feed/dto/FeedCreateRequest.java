package com.team1.otvoo.feed.dto;

import java.util.List;
import java.util.UUID;

public record FeedCreateRequest(
    UUID authorId,
    UUID weatherId,
    List<String> clothesIds,
    String content
) {

}

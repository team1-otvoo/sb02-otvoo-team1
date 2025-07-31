package com.team1.otvoo.feed.dto;

import com.team1.otvoo.recommendation.dto.OotdDto;
import com.team1.otvoo.user.dto.AuthorDto;
import com.team1.otvoo.weather.dto.WeatherSummaryDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record FeedDto(
    UUID id,
    Instant createdAt,
    Instant updatedAt,
    AuthorDto author,
    WeatherSummaryDto weather,
    List<OotdDto> ootds,
    String content,
    long likeCount,
    long commentCount,
    boolean likedByMe
) {

}

package com.team1.otvoo.feed.dto;

import com.team1.otvoo.clothes.dto.OotdDto;
import com.team1.otvoo.user.dto.AuthorDto;
import com.team1.otvoo.weather.dto.WeatherSummaryDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
@Setter
@Builder
@AllArgsConstructor
public class FeedDto {
  private UUID id;
  private Instant createdAt;
  private Instant updatedAt;
  private AuthorDto author;
  private WeatherSummaryDto weather;
  private List<OotdDto> ootds;
  private String content;
  private long likeCount;
  private long commentCount;
  private boolean likedByMe;
}

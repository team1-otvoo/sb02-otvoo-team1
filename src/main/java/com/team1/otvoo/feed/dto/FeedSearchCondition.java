package com.team1.otvoo.feed.dto;

import com.team1.otvoo.weather.entity.PrecipitationType;
import com.team1.otvoo.weather.entity.SkyStatus;
import java.util.UUID;
import lombok.Builder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Builder
public record FeedSearchCondition(
    String cursor,
    UUID idAfter,
    int limit,
    String sortBy,
    String sortDirection,
    String keywordLike,
    SkyStatus skyStatusEqual,
    PrecipitationType precipitationTypeEqual,
    UUID authorIdEqual
) {

  // 페이징 및 정렬 정보 전달용
  public Pageable toPageable() {
    Sort sort = "likeCount".equalsIgnoreCase(sortBy)
        ? Sort.by(Sort.Direction.fromString(normalizeDirection(sortDirection)), "likeCount")
        : Sort.by(Sort.Direction.fromString(normalizeDirection(sortDirection)), "createdAt");

    return PageRequest.of(0, limit, sort);
  }

  // Sort.Direction은 "ASC", "DESC만 인식 가능하므로 변환
  private String normalizeDirection(String sortDirection) {
    String direction = sortDirection.toUpperCase();
    if (direction.equals("DESCENDING")) {
      return "DESC";
    } else if (direction.equals("ASCENDING")) {
      return "ASC";
    }
    return direction;
  }

}

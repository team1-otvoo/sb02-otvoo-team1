package com.team1.otvoo.feed.mapper;

import com.team1.otvoo.feed.dto.FeedDto;
import com.team1.otvoo.feed.dto.FeedDtoCursorResponse;
import com.team1.otvoo.feed.dto.FeedSearchCondition;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

@Component
public class FeedPageResponseMapper {
  public FeedDtoCursorResponse toPageResponse(
      Slice<FeedDto> feedDtoSlice,
      FeedSearchCondition searchCondition) {

    List<FeedDto> content = feedDtoSlice.getContent();
    FeedDto last = content.isEmpty() ? null : content.get(content.size() - 1);

    String nextCursor = null;
    if (last != null) {
      nextCursor = "likeCount".equalsIgnoreCase(searchCondition.sortBy())
          // 복합 커서 생성
          ? last.getLikeCount() + "_" + last.getCreatedAt().toString()
          : last.getCreatedAt().toString();
    }

    UUID nextIdAfter = null;
    if (last != null) {
      nextIdAfter = last.getId();
    }

    return new FeedDtoCursorResponse(
        content,
        nextCursor,
        nextIdAfter,
        feedDtoSlice.hasNext(),
        null,
        searchCondition.sortBy(),
        searchCondition.sortDirection()
    );
  }
}

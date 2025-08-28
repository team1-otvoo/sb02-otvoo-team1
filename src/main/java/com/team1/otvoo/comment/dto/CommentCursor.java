package com.team1.otvoo.comment.dto;

import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record CommentCursor(
    String cursor,
    UUID idAfter,
    int limit
) {

  // 페이징 정보 전달용
  public Pageable toPageable() {
    Sort sort = Sort.by(Sort.Direction.fromString("ASC"), "createdAt");
    return PageRequest.of(0, limit, sort);
  }
}

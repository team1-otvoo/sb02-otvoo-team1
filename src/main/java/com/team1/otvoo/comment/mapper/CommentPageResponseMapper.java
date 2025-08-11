package com.team1.otvoo.comment.mapper;

import com.team1.otvoo.comment.dto.CommentCursor;
import com.team1.otvoo.comment.dto.CommentDto;
import com.team1.otvoo.comment.dto.CommentDtoCursorResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

@Component
public class CommentPageResponseMapper {
  public CommentDtoCursorResponse toPageResponse(
      Slice<CommentDto> commentDtoSlice,
      CommentCursor commentCursor) {

    List<CommentDto> content = commentDtoSlice.getContent();
    CommentDto last = content.isEmpty() ? null : content.get(content.size() - 1);

    String nextCursor = null;
    if (last != null) {
      nextCursor = last.createdAt().toString();
    }

    UUID nextIdAfter = null;
    if (last != null) {
      nextIdAfter = last.id();
    }

    return new CommentDtoCursorResponse(
        content,
        nextCursor,
        nextIdAfter,
        commentDtoSlice.hasNext(),
        null,
        "createdAt",
        "ASCENDING"
    );
  }
}

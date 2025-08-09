package com.team1.otvoo.comment.repository;

import com.team1.otvoo.comment.dto.CommentCursor;
import com.team1.otvoo.comment.dto.CommentDto;
import java.util.UUID;
import org.springframework.data.domain.Slice;

public interface CommentRepositoryCustom {
  Slice<CommentDto> findCommentsWithCursor(CommentCursor commentCursor, UUID feedId);

}

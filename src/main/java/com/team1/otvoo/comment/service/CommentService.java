package com.team1.otvoo.comment.service;

import com.team1.otvoo.comment.dto.CommentCreateRequest;
import com.team1.otvoo.comment.dto.CommentCursor;
import com.team1.otvoo.comment.dto.CommentDto;
import java.util.UUID;
import org.springframework.data.domain.Slice;

public interface CommentService {
  CommentDto create(CommentCreateRequest request);
  Slice<CommentDto> getCommentsWithCursor(CommentCursor cursor, UUID feedId);
}

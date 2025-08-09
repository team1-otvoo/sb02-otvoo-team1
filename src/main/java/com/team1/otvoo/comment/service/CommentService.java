package com.team1.otvoo.comment.service;

import com.team1.otvoo.comment.dto.CommentCreateRequest;
import com.team1.otvoo.comment.dto.CommentDto;

public interface CommentService {
  CommentDto create(CommentCreateRequest request);
}

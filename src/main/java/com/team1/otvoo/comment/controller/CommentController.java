package com.team1.otvoo.comment.controller;

import com.team1.otvoo.comment.dto.CommentCreateRequest;
import com.team1.otvoo.comment.dto.CommentDto;
import com.team1.otvoo.comment.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feeds")
@Slf4j
public class CommentController {
  private final CommentService commentService;

  @PostMapping("/{feedId}/comments")
  public ResponseEntity<CommentDto> create(@RequestBody CommentCreateRequest request) {
    log.info("댓글 등록 요청 - feedId: {}, authorId: {}", request.feedId(), request.authorId());

    CommentDto createdComment = commentService.create(request);

    log.info("댓글 등록 완료 - feedId: {}, authorId: {}", request.feedId(), request.authorId());
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(createdComment);
  }
}

package com.team1.otvoo.comment.controller;

import com.team1.otvoo.comment.dto.CommentCreateRequest;
import com.team1.otvoo.comment.dto.CommentCursor;
import com.team1.otvoo.comment.dto.CommentDto;
import com.team1.otvoo.comment.dto.CommentDtoCursorResponse;
import com.team1.otvoo.comment.mapper.CommentPageResponseMapper;
import com.team1.otvoo.comment.service.CommentService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feeds")
@Slf4j
public class CommentController {
  private final CommentService commentService;
  private final CommentPageResponseMapper pageResponseMapper;

  @PostMapping("/{feedId}/comments")
  public ResponseEntity<CommentDto> create(@RequestBody CommentCreateRequest request) {
    log.info("댓글 등록 요청 - feedId: {}, authorId: {}", request.feedId(), request.authorId());

    CommentDto createdComment = commentService.create(request);

    log.info("댓글 등록 완료 - feedId: {}, authorId: {}", request.feedId(), request.authorId());
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(createdComment);
  }

  @GetMapping("/{feedId}/comments")
  public ResponseEntity<CommentDtoCursorResponse> getComments(
      @PathVariable("feedId")UUID feedId,
      @RequestParam(name = "cursor", required = false) String cursor,
      @RequestParam(name = "idAfter", required = false) UUID idAfter,
      @RequestParam(name = "limit", defaultValue = "5") int limit
  ) {
    CommentCursor commentCursor = new CommentCursor(cursor, idAfter, limit);

    Slice<CommentDto> commentDtoList = commentService.getCommentsWithCursor(commentCursor, feedId);

    return ResponseEntity.ok()
        .body(pageResponseMapper.toPageResponse(commentDtoList, commentCursor));
  }
}

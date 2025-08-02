package com.team1.otvoo.feed.controller;

import com.team1.otvoo.feed.dto.FeedCreateRequest;
import com.team1.otvoo.feed.dto.FeedDto;
import com.team1.otvoo.feed.dto.FeedUpdateRequest;
import com.team1.otvoo.feed.service.FeedService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/feeds")
public class FeedController {
  private final FeedService feedService;

  @PostMapping
  public ResponseEntity<FeedDto> create(@RequestBody @Valid  FeedCreateRequest request) {
    log.info("피드 생성 요청 - authorId: {}", request.authorId());

    FeedDto feedDto = feedService.create(request);

    log.info("피드 생성 완료 - authorId: {}, feedId: {}", feedDto.author(), feedDto.id());

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(feedDto);
  }

  @PatchMapping("/{feedId}")
  public ResponseEntity<FeedDto> update(@PathVariable UUID feedId,
      @RequestBody @Valid FeedUpdateRequest request) {
    log.info("피드 수정 요청 - feedId: {}", feedId);

    FeedDto feedDto = feedService.update(feedId, request);

    log.info("피드 수정 완료 - feedId: {}", feedId);
    return ResponseEntity.ok(feedDto);
  }

  @DeleteMapping("/{feedId}")
  public ResponseEntity<Void> delete(@PathVariable UUID feedId) {
    log.info("피드 삭제 요청 - feedId: {}", feedId);

    feedService.delete(feedId);

    log.info("피드 삭제 완료 - feedId: {}", feedId);
    return ResponseEntity.noContent().build();
  }
}
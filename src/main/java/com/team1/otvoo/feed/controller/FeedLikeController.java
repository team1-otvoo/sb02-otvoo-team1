package com.team1.otvoo.feed.controller;

import com.team1.otvoo.feed.service.FeedLikeService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feeds")
@Slf4j
@RequiredArgsConstructor
public class FeedLikeController {
  private final FeedLikeService feedLikeService;

  @PostMapping("/{feedId}/like")
  public ResponseEntity<Void> like(@PathVariable("feedId") UUID feedId) {
    log.info("피드 좋아요 요청 - feedId: {}", feedId);

    feedLikeService.create(feedId);

    log.info("피드 좋아요 처리 완료 - feedId: {}", feedId);
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .build();
  }

  @DeleteMapping("/{feedId}/like")
  public ResponseEntity<Void> unlike(@PathVariable("feedId") UUID feedId) {
    log.info("피드 좋아요 취소 요청 - feedId: {}", feedId);

    feedLikeService.delete(feedId);

    log.info("피드 좋아요 취소 처리 완료 - feedId: {}", feedId);
    return ResponseEntity
        .noContent()
        .build();
  }
}

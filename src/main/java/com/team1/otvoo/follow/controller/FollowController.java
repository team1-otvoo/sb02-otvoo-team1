package com.team1.otvoo.follow.controller;

import com.team1.otvoo.follow.dto.FollowCreateRequest;
import com.team1.otvoo.follow.dto.FollowDto;
import com.team1.otvoo.follow.dto.FollowListResponse;
import com.team1.otvoo.follow.service.FollowService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/follows")
public class FollowController {

  private final FollowService followService;

  @PostMapping
  public ResponseEntity<FollowDto> follow(
      @RequestBody @Valid FollowCreateRequest request) {
    log.info("팔로우 생성 요청: {}", request);
    FollowDto createdFollow = followService.create(request);
    log.debug("팔로우 생성 응답: {}", createdFollow);
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(createdFollow);
  }

  @DeleteMapping("/{followId}")
  public ResponseEntity<Void> unfollow(@PathVariable UUID followId) {
    log.info("팔로우 취소 요청: id={}", followId);
    followService.delete(followId);
    log.debug("팔로우 취소 완료");
    return ResponseEntity.noContent().build();
  }





}

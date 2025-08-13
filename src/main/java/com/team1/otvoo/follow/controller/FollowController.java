package com.team1.otvoo.follow.controller;

import com.team1.otvoo.follow.dto.FollowCreateRequest;
import com.team1.otvoo.follow.dto.FollowDto;
import com.team1.otvoo.follow.dto.FollowListResponse;
import com.team1.otvoo.follow.dto.FollowSummaryDto;
import com.team1.otvoo.follow.service.FollowService;
import com.team1.otvoo.security.CustomUserDetails;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

  @GetMapping("/summary")
  public ResponseEntity<FollowSummaryDto> getSummary(
      @RequestParam("userId") UUID userId,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    log.info("팔로우 요약 정보 조회 요청: userId={}", userId);
    UUID myId = userDetails.getUser().getId();
    FollowSummaryDto summary = followService.getSummary(userId, myId);
    log.debug("팔로우 요약 정보 조회 응답: followeeId={}", summary.followeeId());
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(summary);
  }

  @GetMapping("/followings")
  public ResponseEntity<FollowListResponse> getFollowingList(
      @RequestParam("followerId") UUID followerId,
      @RequestParam(value = "cursor", required = false) String cursor,
      @RequestParam(value = "idAfter", required = false) UUID idAfter,
      @RequestParam(value = "limit", defaultValue = "20") int limit,
      @RequestParam(value = "nameLike", required = false) String nameLike) {
    log.info("팔로잉 목록 조회 요청: followerId={}, cursor={}, idAfter={}, limit={}, nameLike={}",
        followerId, cursor, idAfter, limit, nameLike);
    FollowListResponse followingList = followService.getFollowingList(followerId, cursor, idAfter, limit, nameLike);
    log.debug("팔로잉 목록 조회 응답: totalCount={}", followingList.totalCount());
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(followingList);
  }

  @GetMapping("/followers")
  public ResponseEntity<FollowListResponse> getFollowerList(
      @RequestParam("followeeId") UUID followeeId,
      @RequestParam(value = "cursor", required = false) String cursor,
      @RequestParam(value = "idAfter", required = false) UUID idAfter,
      @RequestParam(value = "limit", defaultValue = "20") int limit,
      @RequestParam(value = "nameLike", required = false) String nameLike) {
    log.info("팔로워 목록 조회 요청: followeeId={}, cursor={}, idAfter={}, limit={}, nameLike={}",
        followeeId, cursor, idAfter, limit, nameLike);
    FollowListResponse followerList = followService.getFollowerList(followeeId, cursor, idAfter, limit, nameLike);
    log.debug("팔로워 목록 조회 응답: totalCount={}", followerList.totalCount());
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(followerList);
  }

  @DeleteMapping("/{followId}")
  public ResponseEntity<Void> unfollow(@PathVariable UUID followId) {
    log.info("팔로우 취소 요청: id={}", followId);
    followService.delete(followId);
    log.debug("팔로우 취소 완료");
    return ResponseEntity.noContent().build();
  }
}

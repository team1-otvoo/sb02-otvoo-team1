package com.team1.otvoo.directmessage.controller;

import com.team1.otvoo.directmessage.dto.DirectMessageDtoCursorResponse;
import com.team1.otvoo.directmessage.service.DirectMessageService;
import com.team1.otvoo.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/direct-messages")
@RequiredArgsConstructor
public class DirectMessageController {

  private final DirectMessageService directMessageService;

  @GetMapping
  public ResponseEntity<DirectMessageDtoCursorResponse> getDirectMessages(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestParam UUID userId,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) String idAfter,
      @RequestParam(required = false, defaultValue = "10") int limit) {

    UUID currentUserId = userDetails.getUser().getId();

    log.info("✅ DM API 요청: currentUserId={}, otherUserId={}, cursor={}, idAfter={}, limit={}",
        currentUserId, userId, cursor, idAfter, limit);

    DirectMessageDtoCursorResponse response =
        directMessageService.getDirectMessagesBetweenUsers(currentUserId, userId, cursor, idAfter, limit);

    log.info("✅ DM API 응답: dataCount={}, hasNext={}, nextCursor={}, nextIdAfter={}",
        response.data().size(), response.hasNext(), response.nextCursor(), response.nextIdAfter());

    return ResponseEntity.ok(response);
  }

}
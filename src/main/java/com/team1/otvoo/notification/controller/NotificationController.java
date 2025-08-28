package com.team1.otvoo.notification.controller;

import com.team1.otvoo.notification.dto.NotificationDtoCursorResponse;
import com.team1.otvoo.notification.service.NotificationService;
import com.team1.otvoo.security.CustomUserDetails;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

  private final NotificationService notificationService;

  @GetMapping
  public ResponseEntity<NotificationDtoCursorResponse> getList(
      @RequestParam(value = "cursor", required = false) String cursor,
      @RequestParam(value = "idAfter", required = false) UUID idAfter,
      @RequestParam(value = "limit", defaultValue = "20") int limit,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    UUID receiverId = userDetails.getUser().getId();
    log.info("알림 목록 조회 요청: receiverId={}, cursor={}, idAfter={}, limit={}}", receiverId, cursor, idAfter, limit);
    NotificationDtoCursorResponse notificationList = notificationService.getList(receiverId, cursor, idAfter, limit);
    log.debug("알림 목록 조회 응답: totalCount={}", notificationList.totalCount());
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(notificationList);
  }

  @DeleteMapping("/{notificationId}")
  public ResponseEntity<Void> read(
      @PathVariable UUID notificationId,
      @AuthenticationPrincipal CustomUserDetails userDetails
  ) {
    UUID userId = userDetails.getUser().getId();
    log.info("알림 읽음 처리 요청: id={}, userId={}", notificationId, userId);
    notificationService.readNotification(notificationId, userId);
    log.debug("알림 읽음 처리 완료");
    return ResponseEntity.noContent().build();
  }

}

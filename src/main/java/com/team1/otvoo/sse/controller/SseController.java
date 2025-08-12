package com.team1.otvoo.sse.controller;

import com.team1.otvoo.security.CustomUserDetails;
import com.team1.otvoo.sse.service.SseService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/sse")
public class SseController {

  private final SseService sseService;

  @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter subscribe(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestParam(value = "LastEventId", required = false) UUID lastEventId
  ) {
    log.info("Sse 연결 요청: {}", lastEventId);
    UUID userId = userDetails.getUser().getId();
    return sseService.connect(userId, lastEventId);
  }

}

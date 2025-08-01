package com.team1.otvoo.auth.controller;

import com.team1.otvoo.auth.dto.CsrfTokenResponse;
import com.team1.otvoo.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @GetMapping("/csrf-token")
  public ResponseEntity<CsrfTokenResponse> getCsrfToken(HttpServletRequest request) {
    CsrfTokenResponse csrfToken = authService.getCsrfToken(request);

    log.info("✅ CSRF 토큰 API 호출 성공");

    return ResponseEntity.status(HttpStatus.OK).body(csrfToken);
  }
}
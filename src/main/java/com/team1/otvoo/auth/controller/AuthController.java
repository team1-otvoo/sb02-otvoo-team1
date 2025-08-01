package com.team1.otvoo.auth.controller;

import com.team1.otvoo.auth.dto.CsrfTokenResponse;
import com.team1.otvoo.auth.dto.SignInRequest;
import com.team1.otvoo.auth.dto.SignInResponse;
import com.team1.otvoo.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

  @PostMapping("/sign-in")
  public ResponseEntity<SignInResponse> signIn(@RequestBody SignInRequest request) {
    log.info("🔷 로그인 시도: email={}", request.email());
    SignInResponse response = authService.signIn(request);

    log.info("✅ 로그인 완료: accessToken={}, refreshToken={}", response.accessToken(), response.refreshToken());
    return ResponseEntity.ok(response);
  }
}
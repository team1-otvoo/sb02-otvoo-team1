package com.team1.otvoo.auth.controller;

import com.team1.otvoo.auth.dto.CsrfTokenResponse;
import com.team1.otvoo.auth.dto.ResetPasswordRequest;
import com.team1.otvoo.auth.dto.SignInResponse;
import com.team1.otvoo.auth.service.AuthService;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

  @GetMapping("/me")
  public ResponseEntity<String> getAccessTokenByRefreshToken(
      @CookieValue(value = "refresh_token", required = false) String refreshToken
  ) {
    log.info("🟡 액세스 토큰 요청 - 쿠키에서 refresh_token 조회됨");

    if (refreshToken == null || refreshToken.isBlank()) {
      log.warn("❌ 리프레시 토큰이 누락되었습니다.");
      throw new RestException(ErrorCode.UNAUTHORIZED, Map.of("reason", "리프레시 토큰이 필요합니다."));
    }

    String accessToken = authService.getAccessTokenByRefreshToken(refreshToken);

    log.info("✅ 액세스 토큰 조회 완료");
    return ResponseEntity.ok(accessToken);
  }

  @PostMapping("/refresh")
  public ResponseEntity<SignInResponse> refreshToken(
      @CookieValue(value = "refresh_token", required = false) String refreshToken
  ) {
    log.info("🔄 토큰 재발급 요청");

    if (refreshToken == null || refreshToken.isBlank()) {
      log.warn("❌ 리프레시 토큰 누락");
      throw new RestException(ErrorCode.UNAUTHORIZED, Map.of("reason", "리프레시 토큰이 필요합니다."));
    }

    SignInResponse tokens = authService.refreshToken(refreshToken);

    log.info("✅ 토큰 재발급 성공: accessToken={}, refreshToken={}", tokens.accessToken(), tokens.refreshToken());

    return ResponseEntity.ok(tokens);
  }

  @PostMapping("/reset-password")
  public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
    log.info("🔧 비밀번호 초기화 요청: email={}", request.email());

    authService.resetPassword(request.email());

    log.info("✅ 임시 비밀번호 이메일 전송 완료");
    return ResponseEntity.noContent().build();
  }
}
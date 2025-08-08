package com.team1.otvoo.security;

import com.team1.otvoo.auth.token.AccessTokenStore;
import com.team1.otvoo.auth.token.RefreshTokenStore;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtLogoutHandler implements LogoutHandler {

  private final JwtTokenProvider jwtTokenProvider;
  private final RefreshTokenStore refreshTokenStore;
  private final AccessTokenStore accessTokenStore;

  @Override
  public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
    String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    String refreshToken = null;

    if (request.getCookies() != null) {
      for (var cookie : request.getCookies()) {
        if ("refresh_token".equals(cookie.getName())) {
          refreshToken = cookie.getValue();
          break;
        }
      }
    }

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      log.warn("❌ 로그아웃 실패 - Authorization 헤더 없음 또는 형식 오류");
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    String accessToken = authHeader.substring(7).trim();

    if (!jwtTokenProvider.validateToken(accessToken)) {
      log.warn("❌ 로그아웃 실패 - 유효하지 않은 Access Token");
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }

    UUID userId = jwtTokenProvider.getUserIdFromToken(accessToken);
    if (userId == null) {
      log.warn("❌ 로그아웃 실패 - Access Token에서 userId 추출 실패");
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }

    String storedRefreshToken = refreshTokenStore.get(userId);
    if (storedRefreshToken == null || refreshToken == null || !storedRefreshToken.equals(refreshToken)) {
      log.warn("❌ 로그아웃 실패 - Refresh Token 불일치 또는 존재하지 않음");
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }

    log.info("🚪 로그아웃 처리 시작: userId={}", userId);

    refreshTokenStore.remove(userId);
    log.debug("✅ RefreshToken 제거 완료: userId={}", userId);

    long expiration = jwtTokenProvider.getExpirationSecondsLeft(accessToken);
    accessTokenStore.blacklistAccessToken(accessToken, expiration);
    log.debug("✅ AccessToken 블랙리스트 등록 완료: 만료시간(ms)={}", expiration);

    Cookie deleteCookie = new Cookie("refresh_token", null);
    deleteCookie.setPath("/");
    deleteCookie.setMaxAge(0);
    deleteCookie.setHttpOnly(true);
    deleteCookie.setSecure(false);
    response.addCookie(deleteCookie);
    log.debug("✅ refresh_token 쿠키 삭제 완료");

    try {
      response.setStatus(HttpServletResponse.SC_OK);
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");
      response.getWriter().write("{\"logout\":\"success\"}");
      log.info("✅ 로그아웃 완료 및 응답 전송");
    } catch (IOException e) {
      log.error("⚠️ 로그아웃 응답 전송 실패", e);
    }
  }
}
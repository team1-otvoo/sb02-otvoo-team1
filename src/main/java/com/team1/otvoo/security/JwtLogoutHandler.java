package com.team1.otvoo.security;

import com.team1.otvoo.auth.token.AccessTokenStore;
import com.team1.otvoo.auth.token.RefreshTokenStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

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

    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      String rawToken = authHeader.substring(7).trim();
      String accessToken = extractAccessToken(rawToken);
      String refreshToken = extractRefreshToken(rawToken);

      if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
        String userId = jwtTokenProvider.getUserIdFromToken(accessToken);

        String storedRefreshToken = refreshTokenStore.get(userId);
        if (storedRefreshToken == null || refreshToken == null || !storedRefreshToken.equals(refreshToken)) {
          log.warn("❌ 로그아웃 실패 - Refresh Token 불일치 또는 존재하지 않음");
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          return;
        }

        log.info("🚪 로그아웃 처리 시작: userId={}", userId);

        refreshTokenStore.remove(userId);
        log.debug("✅ RefreshTokenStore 에서 userId={} 토큰 제거 완료", userId);

        long expiration = jwtTokenProvider.getExpiration(accessToken);
        accessTokenStore.blacklistAccessToken(accessToken, expiration);
        log.debug("✅ AccessToken 블랙리스트 등록 완료: 만료시간={}초", expiration);

        try {
          response.setStatus(HttpServletResponse.SC_OK);
          response.setContentType("text/plain");
          response.setCharacterEncoding("UTF-8");
          response.getWriter().write("logout=success");
          log.info("✅ 로그아웃 응답 전송 완료");
        } catch (IOException e) {
          log.error("⚠️ 로그아웃 응답 전송 중 오류 발생", e);
        }
      } else {
        log.warn("❌ 로그아웃 실패 - 유효하지 않은 Access Token");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      }
    } else {
      log.warn("❌ 로그아웃 실패 - Authorization 헤더 없음 또는 Bearer 토큰 아님");
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }
  }

  private String extractAccessToken(String rawToken) {
    if (rawToken.startsWith("accessToken=")) {
      for (String part : rawToken.split("&")) {
        if (part.startsWith("accessToken=")) {
          return part.substring("accessToken=".length());
        }
      }
      return null;
    }
    return rawToken.contains("&") ? rawToken.split("&")[0].trim() : rawToken;
  }

  private String extractRefreshToken(String rawToken) {
    if (rawToken.contains("refreshToken=")) {
      for (String part : rawToken.split("&")) {
        if (part.startsWith("refreshToken=")) {
          return part.substring("refreshToken=".length());
        }
      }
    }
    return null;
  }
}
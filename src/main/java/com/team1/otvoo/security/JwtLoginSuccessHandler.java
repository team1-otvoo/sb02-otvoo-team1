package com.team1.otvoo.security;

import com.team1.otvoo.auth.token.AccessTokenStore;
import com.team1.otvoo.auth.token.RedisRefreshTokenStore;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtLoginSuccessHandler implements AuthenticationSuccessHandler {

  private final JwtTokenProvider jwtTokenProvider;
  private final RedisRefreshTokenStore refreshTokenStore;
  private final AccessTokenStore accessTokenStore;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException {
    CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
    UUID userId = userDetails.getUser().getId();
    String role = userDetails.getRole();
    String email = userDetails.getUser().getEmail();

    log.info("🔐 로그인 성공: userId={}", userId);

    String accessToken = jwtTokenProvider.createAccessToken(userId, role, email);
    String refreshToken = jwtTokenProvider.createRefreshToken(userId, role, email);

    long expirationSeconds = jwtTokenProvider.getExpirationSecondsLeft(accessToken);
    accessTokenStore.blacklistAccessToken(accessToken, expirationSeconds);
    accessTokenStore.save(userId, accessToken, expirationSeconds);

    Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
    refreshCookie.setHttpOnly(true);
    refreshCookie.setSecure(false);
    refreshCookie.setPath("/");
    refreshCookie.setMaxAge((int) jwtTokenProvider.getRefreshTokenValidityInSeconds());
    response.addCookie(refreshCookie);

    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write("\"" + accessToken + "\"");

    log.debug("✅ 로그인 응답 전송 완료: accessToken={}, refreshToken(COOKIE)", accessToken);
  }
}
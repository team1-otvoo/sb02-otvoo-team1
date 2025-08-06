package com.team1.otvoo.security;

import com.team1.otvoo.auth.token.RedisTemporaryPasswordStore;
import com.team1.otvoo.auth.token.RedisRefreshTokenStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtLoginSuccessHandler implements AuthenticationSuccessHandler {

  private final JwtTokenProvider jwtTokenProvider;
  private final RedisRefreshTokenStore refreshTokenStore;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException {
    CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
    String username = userDetails.getUsername();

    log.info("🔐 로그인 성공: username={}", username);

    boolean isUsingTemporaryPassword = userDetails.isUsingTemporaryPassword();

    String accessToken = jwtTokenProvider.createAccessToken(username);
    String refreshToken = jwtTokenProvider.createRefreshToken(username);

    refreshTokenStore.save(username, refreshToken);
    log.debug("💾 RefreshToken Redis 저장: key=refreshToken:{}, 만료시간={}", username, jwtTokenProvider.getExpiration(refreshToken));

    response.setContentType("text/plain");
    response.setCharacterEncoding("UTF-8");
    response.getWriter().write(String.format(
        "accessToken=%s&refreshToken=%s&isUsingTemporaryPassword=%b",
        accessToken, refreshToken, isUsingTemporaryPassword));

    log.debug("✅ 로그인 응답 전송 완료");
  }
}
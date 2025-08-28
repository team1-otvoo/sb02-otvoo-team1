package com.team1.otvoo.security;

import com.team1.otvoo.auth.token.AccessTokenStore;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.ProfileRepository;
import com.team1.otvoo.user.repository.UserRepository;
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
public class OAuth2JwtLoginSuccessHandler implements AuthenticationSuccessHandler {

  private final JwtTokenProvider jwtTokenProvider;
  private final AccessTokenStore accessTokenStore;
  private final UserRepository userRepository;
  private final ProfileRepository profileRepository;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException {
    CustomOAuth2User customUser = (CustomOAuth2User) authentication.getPrincipal();
    User user = customUser.getUser();
    UUID userId = user.getId();
    String role = user.getRole().name();
    String email = user.getEmail();

    log.info("🔐 OAuth2 로그인 성공: userId={}, email={}, role={}", userId, email, role);

    String accessToken = jwtTokenProvider.createAccessToken(userId, role, email);
    String refreshToken = jwtTokenProvider.createRefreshToken(userId, role, email);

    long expirationSeconds = jwtTokenProvider.getExpirationSecondsLeft(accessToken);
    accessTokenStore.save(userId, accessToken, expirationSeconds);

    Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
    refreshCookie.setHttpOnly(true);
    refreshCookie.setSecure(true);
    refreshCookie.setPath("/");
    refreshCookie.setMaxAge((int) jwtTokenProvider.getRefreshTokenValidityInSeconds());
    response.addCookie(refreshCookie);

    response.sendRedirect("/");

    log.debug("✅ OAuth2 로그인 완료");
  }
}

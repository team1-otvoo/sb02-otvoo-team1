package com.team1.otvoo.auth.service;

import com.team1.otvoo.auth.dto.SignInRequest;
import com.team1.otvoo.auth.dto.SignInResponse;
import com.team1.otvoo.auth.token.RefreshTokenStore;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.security.JwtTokenProvider;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Service;
import com.team1.otvoo.auth.dto.CsrfTokenResponse;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final UserRepository userRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final PasswordEncoder passwordEncoder;
  private final RefreshTokenStore refreshTokenStore;

  @Override
  public CsrfTokenResponse getCsrfToken(HttpServletRequest request) {
    CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());

    if (csrfToken == null) {
      log.error("❌ CSRF 토큰이 존재하지 않습니다.");
      throw new RestException(ErrorCode.INTERNAL_SERVER_ERROR, Map.of("detail", "CSRF 토큰을 찾을 수 없습니다."));
    }

    log.info("✅ CSRF 토큰 조회 성공: headerName={}, parameterName={}", csrfToken.getHeaderName(), csrfToken.getParameterName());
    return new CsrfTokenResponse(
        csrfToken.getHeaderName(),
        csrfToken.getToken(),
        csrfToken.getParameterName()
    );
  }

  @Override
  public SignInResponse signIn(SignInRequest request) {
    log.info("🔐 로그인 시도: email={}", request.email());

    User user = userRepository.findByEmail(request.email())
        .orElseThrow(() -> {
          log.warn("❌ 로그인 실패 - 존재하지 않는 이메일: {}", request.email());
          return new RestException(ErrorCode.INVALID_CREDENTIALS);
        });

    boolean matches = passwordEncoder.matches(request.password(), user.getPassword());

    if (!matches) {
      log.warn("❌ 로그인 실패 - 비밀번호 불일치: email={}", request.email());
      throw new RestException(ErrorCode.INVALID_CREDENTIALS);
    }

    String accessToken = jwtTokenProvider.createAccessToken(user.getId().toString());
    String refreshToken = jwtTokenProvider.createRefreshToken(user.getId().toString());

    refreshTokenStore.save(user.getId().toString(), refreshToken);

    log.info("✅ 로그인 성공: userId={}", user.getId());

    return new SignInResponse(accessToken, refreshToken);
  }
}
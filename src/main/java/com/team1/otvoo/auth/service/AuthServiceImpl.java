package com.team1.otvoo.auth.service;

import com.team1.otvoo.auth.dto.CsrfTokenResponse;
import com.team1.otvoo.auth.dto.SignInResponse;
import com.team1.otvoo.auth.token.RefreshTokenStore;
import com.team1.otvoo.auth.token.TemporaryPassword;
import com.team1.otvoo.auth.token.TemporaryPasswordStore;
import com.team1.otvoo.config.props.AdminProps;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.security.JwtTokenProvider;
import com.team1.otvoo.user.dto.UserDto;
import com.team1.otvoo.user.entity.Profile;
import com.team1.otvoo.user.entity.Role;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.ProfileRepository;
import com.team1.otvoo.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final UserRepository userRepository;
  private final ProfileRepository profileRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final RefreshTokenStore refreshTokenStore;
  private final EmailService emailService;
  private final TemporaryPasswordStore temporaryPasswordStore;
  private final AdminProps adminProps;
  private final PasswordEncoder passwordEncoder;

  private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
  private static final String DIGITS = "0123456789";
  private static final String SPECIAL = "!@#$%^&*";
  private static final String ALL = UPPER + LOWER + DIGITS + SPECIAL;

  private final Random random = new SecureRandom();

  @Transactional
  @Override
  public void initAdmin() {

    String email = adminProps.getEmail();
    String username = adminProps.getName();
    String rawPassword = adminProps.getPassword();
    String encodedPassword = passwordEncoder.encode(rawPassword);

    if (userRepository.existsByEmail(email)) {
      log.info("관리자 계정({})이 이미 존재하여 초기화를 건너뜁니다.", email);
      return;
    }

    User admin = new User(email, encodedPassword);
    admin.updateRole(Role.ADMIN);
    Profile profile = new Profile(username, admin);

    // 다중 인스턴스 환경일 시 수정 필요(추가 처리 필요)
    userRepository.save(admin);
    profileRepository.save(profile);
    log.info("admin 계정이 초기화 되었습니다.");
  }

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
  public String getAccessTokenByRefreshToken(String refreshToken) {
    if (!jwtTokenProvider.validateToken(refreshToken)) {
      log.warn("❌ 유효하지 않은 리프레시 토큰");
      throw new RestException(ErrorCode.UNAUTHORIZED, Map.of("reason", "리프레시 토큰이 유효하지 않습니다."));
    }

    UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
    String storedToken = refreshTokenStore.get(userId);
    if (storedToken == null || !storedToken.equals(refreshToken)) {
      log.warn("❌ 저장된 리프레시 토큰과 일치하지 않음");
      throw new RestException(ErrorCode.UNAUTHORIZED, Map.of("reason", "토큰 불일치 또는 만료되었습니다."));
    }

    User user = userRepository.findById(userId).orElseThrow(() -> new RestException(ErrorCode.NOT_FOUND, Map.of("reason", "사용자를 찾을 수 없습니다.")));
    String role = user.getRole().name();
    String name = user.getEmail();

    if (user.isLocked()) {
      // 저장된 리프레시 토큰을 삭제하여 더 이상 사용할 수 없게 만듭니다.
      refreshTokenStore.remove(userId);
      log.warn("❌ 잠긴 사용자에 대한 토큰 재발급 시도: {}", user.getEmail());
      throw new RestException(ErrorCode.UNAUTHORIZED, Map.of("reason", "계정이 잠겨있어 토큰을 재발급할 수 없습니다."));
    }

    return jwtTokenProvider.createAccessToken(userId, role, user.getEmail());
  }

  @Override
  public SignInResponse refreshToken(String refreshToken) {
    log.info("🔄 토큰 재발급 시도");

    if (!jwtTokenProvider.validateToken(refreshToken)) {
      log.warn("❌ 유효하지 않은 리프레시 토큰");
      throw new RestException(ErrorCode.UNAUTHORIZED, Map.of("reason", "리프레시 토큰이 유효하지 않습니다."));
    }

    UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
    String storedToken = refreshTokenStore.get(userId);
    if (storedToken == null || !storedToken.equals(refreshToken)) {
      log.warn("❌ 저장된 토큰과 일치하지 않음");
      throw new RestException(ErrorCode.UNAUTHORIZED, Map.of("reason", "저장된 토큰과 일치하지 않거나 만료되었습니다."));
    }

    User user = userRepository.findById(userId).orElseThrow(() -> new RestException(ErrorCode.NOT_FOUND, Map.of("reason", "사용자를 찾을 수 없습니다.")));
    String role = user.getRole().name();
    String name = user.getEmail();

    if (user.isLocked()) {
      // 저장된 리프레시 토큰을 삭제하여 더 이상 사용할 수 없게 만듭니다.
      refreshTokenStore.remove(userId);
      log.warn("❌ 잠긴 사용자에 대한 토큰 재발급 시도: {}", user.getEmail());
      throw new RestException(ErrorCode.UNAUTHORIZED, Map.of("reason", "계정이 잠겨있어 토큰을 재발급할 수 없습니다."));
    }

    String newAccessToken = jwtTokenProvider.createAccessToken(userId, role, user.getEmail());
    String newRefreshToken = jwtTokenProvider.createRefreshToken(userId, role, user.getEmail());
    refreshTokenStore.save(userId, newRefreshToken);

    log.info("✅ 새로운 토큰 생성 완료");

    return new SignInResponse(newAccessToken, newRefreshToken);
  }

  @Override
  public void resetPassword(String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> {
          log.warn("❌ 비밀번호 초기화 실패 - 존재하지 않는 이메일: {}", email);
          return new RestException(ErrorCode.NOT_FOUND, Map.of("reason", "해당 이메일의 사용자를 찾을 수 없습니다."));
        });

    String tempPassword = generateTemporaryPassword();

    TemporaryPassword temporaryPassword = new TemporaryPassword(tempPassword, System.currentTimeMillis() + Duration.ofMinutes(3).toMillis());

    temporaryPasswordStore.save(user.getId(), temporaryPassword, Duration.ofMinutes(30));

    emailService.sendTemporaryPassword(user.getEmail(), tempPassword);
  }

  private String generateTemporaryPassword() {
    StringBuilder password = new StringBuilder();

    // 필수 문자군 각각 1개 이상 포함
    password.append(UPPER.charAt(random.nextInt(UPPER.length())));
    password.append(LOWER.charAt(random.nextInt(LOWER.length())));
    password.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
    password.append(SPECIAL.charAt(random.nextInt(SPECIAL.length())));

    // 나머지 자리 랜덤 채우기 (총 10자리)
    for (int i = 4; i < 10; i++) {
      password.append(ALL.charAt(random.nextInt(ALL.length())));
    }

    // 문자 섞기
    List<Character> pwdChars = password.chars()
        .mapToObj(c -> (char) c)
        .collect(Collectors.toList());
    Collections.shuffle(pwdChars);

    return pwdChars.stream()
        .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
        .toString();
  }
}
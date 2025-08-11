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
import com.team1.otvoo.user.entity.Profile;
import com.team1.otvoo.user.entity.Role;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.ProfileRepository;
import com.team1.otvoo.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

  @InjectMocks
  private AuthServiceImpl authService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ProfileRepository profileRepository;

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  @Mock
  private RefreshTokenStore refreshTokenStore;

  @Mock
  private TemporaryPasswordStore temporaryPasswordStore;

  @Mock
  private AdminProps adminProps;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private HttpServletRequest httpServletRequest;

  @Mock
  private CsrfToken csrfToken;

  @Mock
  private EmailService emailService;

  private UUID userId;
  private User user;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    user = new User("test@example.com", "encodedPassword");
    user.updateRole(Role.USER);
    ReflectionTestUtils.setField(user, "id", userId);
  }

  @Test
  @DisplayName("관리자 초기화: 이미 존재하면 건너뜀")
  void initAdmin_WhenAdminExists_ShouldSkip() {
    // given
    when(adminProps.getEmail()).thenReturn("admin@example.com");
    when(adminProps.getName()).thenReturn("admin");
    when(adminProps.getPassword()).thenReturn("password");
    when(userRepository.existsByEmail("admin@example.com")).thenReturn(true);

    // when
    authService.initAdmin();

    // then
    verify(userRepository, never()).save(any());
    verify(profileRepository, never()).save(any());
  }

  @Test
  @DisplayName("관리자 초기화: 없으면 저장")
  void initAdmin_WhenAdminNotExists_ShouldSave() {
    // given
    when(adminProps.getEmail()).thenReturn("admin@example.com");
    when(adminProps.getName()).thenReturn("admin");
    when(adminProps.getPassword()).thenReturn("password");
    when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
    when(passwordEncoder.encode("password")).thenReturn("encodedPassword");

    // when
    authService.initAdmin();

    // then
    verify(userRepository).save(argThat(u -> u.getEmail().equals("admin@example.com") && u.getRole() == Role.ADMIN));
    verify(profileRepository).save(argThat(p -> p.getName().equals("admin")));
  }

  @Test
  @DisplayName("CSRF 토큰 정상 조회")
  void getCsrfToken_Success() {
    // given
    when(httpServletRequest.getAttribute(CsrfToken.class.getName())).thenReturn(csrfToken);
    when(csrfToken.getHeaderName()).thenReturn("X-CSRF-TOKEN");
    when(csrfToken.getParameterName()).thenReturn("_csrf");
    when(csrfToken.getToken()).thenReturn("csrfTokenValue");

    // when
    CsrfTokenResponse response = authService.getCsrfToken(httpServletRequest);

    // then
    assertEquals("X-CSRF-TOKEN", response.headerName());
    assertEquals("_csrf", response.parameterName());
    assertEquals("csrfTokenValue", response.token());
  }

  @Test
  @DisplayName("CSRF 토큰 없으면 예외 발생")
  void getCsrfToken_WhenNull_ShouldThrow() {
    // given
    when(httpServletRequest.getAttribute(CsrfToken.class.getName())).thenReturn(null);

    // when & then
    RestException exception = assertThrows(RestException.class, () -> authService.getCsrfToken(httpServletRequest));
    assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, exception.getErrorCode());
  }

  @Test
  @DisplayName("리프레시 토큰으로 액세스 토큰 생성 - 성공")
  void getAccessTokenByRefreshToken_Success() {
    // given
    String refreshToken = "validRefreshToken";
    when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
    when(jwtTokenProvider.getUserIdFromToken(refreshToken)).thenReturn(userId);
    when(refreshTokenStore.get(userId)).thenReturn(refreshToken);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(jwtTokenProvider.createAccessToken(userId, user.getRole().name(), user.getEmail())).thenReturn("newAccessToken");

    // when
    String accessToken = authService.getAccessTokenByRefreshToken(refreshToken);

    // then
    assertEquals("newAccessToken", accessToken);
  }

  @Test
  @DisplayName("리프레시 토큰 유효하지 않으면 예외")
  void getAccessTokenByRefreshToken_InvalidToken_ShouldThrow() {
    // given
    String refreshToken = "invalidToken";
    when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(false);

    // when & then
    RestException exception = assertThrows(RestException.class, () -> authService.getAccessTokenByRefreshToken(refreshToken));
    assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
  }

  @Test
  @DisplayName("리프레시 토큰과 저장된 토큰 불일치 시 예외")
  void getAccessTokenByRefreshToken_TokenMismatch_ShouldThrow() {
    // given
    String refreshToken = "validRefreshToken";
    when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
    when(jwtTokenProvider.getUserIdFromToken(refreshToken)).thenReturn(userId);
    when(refreshTokenStore.get(userId)).thenReturn("differentToken");

    // when & then
    RestException exception = assertThrows(RestException.class, () -> authService.getAccessTokenByRefreshToken(refreshToken));
    assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
  }

  @Test
  @DisplayName("토큰 재발급 성공")
  void refreshToken_Success() {
    // given
    String oldRefreshToken = "oldRefreshToken";
    String newAccessToken = "newAccessToken";
    String newRefreshToken = "newRefreshToken";

    when(jwtTokenProvider.validateToken(oldRefreshToken)).thenReturn(true);
    when(jwtTokenProvider.getUserIdFromToken(oldRefreshToken)).thenReturn(userId);
    when(refreshTokenStore.get(userId)).thenReturn(oldRefreshToken);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(jwtTokenProvider.createAccessToken(userId, user.getRole().name(), user.getEmail())).thenReturn(newAccessToken);
    when(jwtTokenProvider.createRefreshToken(userId, user.getRole().name(), user.getEmail())).thenReturn(newRefreshToken);

    // when
    SignInResponse response = authService.refreshToken(oldRefreshToken);

    // then
    assertEquals(newAccessToken, response.accessToken());
    assertEquals(newRefreshToken, response.refreshToken());
    verify(refreshTokenStore).save(userId, newRefreshToken);
  }

  @Test
  @DisplayName("리셋 비밀번호 - 성공")
  void resetPassword_Success() {
    // given
    String email = "test@example.com";
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    doNothing().when(temporaryPasswordStore).save(any(UUID.class), any(TemporaryPassword.class), any(Duration.class));
    doNothing().when(emailService).sendTemporaryPassword(eq(email), anyString());

    // when
    authService.resetPassword(email);

    // then
    verify(temporaryPasswordStore).save(eq(user.getId()), any(TemporaryPassword.class), any(Duration.class));
    verify(emailService).sendTemporaryPassword(eq(email), anyString());
  }

  @Test
  @DisplayName("리셋 비밀번호 - 이메일 없으면 예외")
  void resetPassword_EmailNotFound_ShouldThrow() {
    // given
    String email = "notfound@example.com";
    when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

    // when & then
    RestException exception = assertThrows(RestException.class, () -> authService.resetPassword(email));
    assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
  }
}
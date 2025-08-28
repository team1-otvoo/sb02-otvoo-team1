package com.team1.otvoo.auth.controller;

import com.team1.otvoo.auth.dto.CsrfTokenResponse;
import com.team1.otvoo.auth.dto.ResetPasswordRequest;
import com.team1.otvoo.auth.dto.SignInResponse;
import com.team1.otvoo.auth.service.AuthService;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @Mock
  private AuthService authService;

  @InjectMocks
  private AuthController authController;

  @Mock
  private HttpServletRequest httpServletRequest;

  @Mock
  private HttpServletResponse httpServletResponse;

  @Test
  @DisplayName("CSRF 토큰 조회 성공")
  void getCsrfToken_ReturnsCsrfTokenResponse() {
    // given
    CsrfTokenResponse expectedToken = new CsrfTokenResponse("X-CSRF-TOKEN", "tokenValue", "_csrf");
    when(authService.getCsrfToken(httpServletRequest)).thenReturn(expectedToken);

    // when
    ResponseEntity<CsrfTokenResponse> response = authController.getCsrfToken(httpServletRequest);

    // then
    assertNotNull(response);
    assertEquals(200, response.getStatusCodeValue());
    assertEquals(expectedToken, response.getBody());

    verify(authService).getCsrfToken(httpServletRequest);
    verifyNoMoreInteractions(authService);
  }

  @Test
  @DisplayName("리프레시 토큰 없으면 UNAUTHORIZED 예외 발생")
  void getAccessTokenByRefreshToken_ThrowsIfNoRefreshToken() {
    // when & then
    RestException exception = assertThrows(RestException.class,
        () -> authController.getAccessTokenByRefreshToken(null));

    assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    assertTrue(exception.getDetails().containsKey("reason"));

    verifyNoInteractions(authService);
  }

  @Test
  @DisplayName("리프레시 토큰 있으면 액세스 토큰 반환")
  void getAccessTokenByRefreshToken_ReturnsAccessToken() {
    // given
    String refreshToken = "validRefreshToken";
    String accessToken = "newAccessToken";
    when(authService.getAccessTokenByRefreshToken(refreshToken)).thenReturn(accessToken);

    // when
    ResponseEntity<String> response = authController.getAccessTokenByRefreshToken(refreshToken);

    // then
    assertNotNull(response);
    assertEquals(200, response.getStatusCodeValue());
    assertEquals(accessToken, response.getBody());

    verify(authService).getAccessTokenByRefreshToken(refreshToken);
    verifyNoMoreInteractions(authService);
  }

  @Test
  @DisplayName("토큰 재발급 - 리프레시 토큰 없으면 UNAUTHORIZED 예외 발생")
  void refreshToken_ThrowsIfNoRefreshToken() {
    // when & then
    RestException exception = assertThrows(RestException.class,
        () -> authController.refreshToken(null, httpServletResponse));

    assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    assertTrue(exception.getDetails().containsKey("reason"));

    verifyNoInteractions(authService);
  }

  @Test
  @DisplayName("토큰 재발급 성공")
  void refreshToken_ReturnsNewTokensAndSetsCookie() {
    // given
    String refreshToken = "validRefreshToken";
    SignInResponse tokens = new SignInResponse("newAccessToken", "newRefreshToken");
    when(authService.refreshToken(refreshToken)).thenReturn(tokens);

    // when
    ResponseEntity<Map<String, String>> response = authController.refreshToken(refreshToken, httpServletResponse);

    // then
    assertNotNull(response);
    assertEquals(200, response.getStatusCodeValue());
    assertEquals(tokens.accessToken(), response.getBody().get("accessToken"));

    verify(authService).refreshToken(refreshToken);
    verify(httpServletResponse).addCookie(argThat(cookie ->
        "refresh_token".equals(cookie.getName()) &&
            tokens.refreshToken().equals(cookie.getValue()) &&
            cookie.isHttpOnly() &&
            "/".equals(cookie.getPath())
    ));
    verifyNoMoreInteractions(authService, httpServletResponse);
  }

  @Test
  @DisplayName("비밀번호 초기화 요청 성공")
  void resetPassword_CallsService() {
    // given
    ResetPasswordRequest request = new ResetPasswordRequest("user@test.com");

    // when
    ResponseEntity<Void> response = authController.resetPassword(request);

    // then
    assertNotNull(response);
    assertEquals(204, response.getStatusCodeValue());

    verify(authService).resetPassword(request.email());
    verifyNoMoreInteractions(authService);
  }
}
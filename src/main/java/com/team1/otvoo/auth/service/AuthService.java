package com.team1.otvoo.auth.service;

import com.team1.otvoo.auth.dto.CsrfTokenResponse;
import com.team1.otvoo.auth.dto.SignInResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
  void initAdmin();
  CsrfTokenResponse getCsrfToken(HttpServletRequest request);
  String getAccessTokenByRefreshToken(String refreshToken);
  SignInResponse refreshToken(String refreshToken);
  void resetPassword(String email);
}
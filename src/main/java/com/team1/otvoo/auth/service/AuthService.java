package com.team1.otvoo.auth.service;

import com.team1.otvoo.auth.dto.CsrfTokenResponse;
import com.team1.otvoo.auth.dto.SignInRequest;
import com.team1.otvoo.auth.dto.SignInResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
  CsrfTokenResponse getCsrfToken(HttpServletRequest request);
  SignInResponse signIn(SignInRequest request);
  void signOut(String accessToken);
  String getAccessTokenByRefreshToken(String refreshToken);
  SignInResponse refreshToken(String refreshToken);
}
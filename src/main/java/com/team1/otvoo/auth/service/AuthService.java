package com.team1.otvoo.auth.service;

import com.team1.otvoo.auth.dto.CsrfTokenResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
  CsrfTokenResponse getCsrfToken(HttpServletRequest request);
}

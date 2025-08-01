package com.team1.otvoo.auth.service;

import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Service;
import com.team1.otvoo.auth.dto.CsrfTokenResponse;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

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
}

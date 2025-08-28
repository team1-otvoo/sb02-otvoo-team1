package com.team1.otvoo.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team1.otvoo.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
public class CustomLoginFailureHandler implements AuthenticationFailureHandler {

  @Override
  public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                      AuthenticationException exception) throws IOException {
    String email = (String) request.getAttribute("email");
    log.warn("❌ 로그인 실패: email={}, error={}", email, exception.getMessage());

    int status = HttpServletResponse.SC_UNAUTHORIZED;
    String message;

    if (exception instanceof CredentialsExpiredException) {
      status = HttpServletResponse.SC_FORBIDDEN;
      message = "임시 비밀번호가 만료되었습니다. 비밀번호 재설정을 다시 시도해주세요.";
    } else if (exception instanceof BadCredentialsException) {
      message = exception.getMessage();
    } else {
      message = "아이디 또는 비밀번호가 올바르지 않습니다.";
    }

    response.setStatus(status);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    Map<String, Object> details = Map.of("email", email == null ? "unknown" : email);
    ErrorResponse errorResponse = new ErrorResponse(
        exception.getClass().getSimpleName(),
        message,
        details
    );

    ObjectMapper mapper = new ObjectMapper();
    response.getWriter().write(mapper.writeValueAsString(errorResponse));

    log.debug("📤 로그인 실패 응답 전송 완료");
  }
}
package com.team1.otvoo.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team1.otvoo.auth.dto.SignInRequest;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JsonUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

  private final ObjectMapper objectMapper;

  @Override
  public Authentication attemptAuthentication(HttpServletRequest request,
                                              HttpServletResponse response) throws AuthenticationException {

    try {
      SignInRequest loginRequest = objectMapper.readValue(request.getInputStream(), SignInRequest.class);

      log.debug("🔐 로그인 요청: {}", loginRequest.email());

      request.setAttribute("email", loginRequest.email());

      UsernamePasswordAuthenticationToken authRequest =
          new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password());

      setDetails(request, authRequest);
      return this.getAuthenticationManager().authenticate(authRequest);

    } catch (IOException e) {
      log.error("❌ 로그인 요청 파싱 실패", e);
      throw new RestException(ErrorCode.JSON_PARSE_ERROR);
    }
  }
}
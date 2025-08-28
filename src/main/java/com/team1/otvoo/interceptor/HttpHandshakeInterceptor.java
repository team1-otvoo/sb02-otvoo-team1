package com.team1.otvoo.interceptor;

import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.security.JwtTokenProvider;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class HttpHandshakeInterceptor implements HandshakeInterceptor {

  private final JwtTokenProvider jwtTokenProvider;

  public HttpHandshakeInterceptor(JwtTokenProvider jwtTokenProvider) {
    this.jwtTokenProvider = jwtTokenProvider;
  }

  @Override
  public boolean beforeHandshake(ServerHttpRequest request,
                                 ServerHttpResponse response,
                                 WebSocketHandler wsHandler,
                                 Map<String, Object> attributes) {

    String token = extractTokenFromQuery(request.getURI());

    if (token == null) {
      token = extractTokenFromCookies(request);
    }

    if (token == null) {
      token = extractTokenFromAuthorizationHeader(request);
    }

    if (token == null) {
      log.error("[HandshakeInterceptor] 토큰 없음: {}", request.getURI());
      throw new RestException(ErrorCode.AUTH_HEADER_MISSING);
    }

    try {
      if (!jwtTokenProvider.validateToken(token)) {
        log.error("[HandshakeInterceptor] 토큰 검증 실패 (만료/위조): {}", token);
        throw new RestException(ErrorCode.INVALID_TOKEN);
      }
    } catch (Exception e) {
      log.error("[HandshakeInterceptor] 토큰 검증 중 오류: {}", e.getMessage());
      throw new RestException(ErrorCode.INTERNAL_SERVER_ERROR, Map.of("error", e.getMessage()));
    }

    UUID userId = jwtTokenProvider.getUserIdFromToken(token);
    attributes.put("userId", userId);
    attributes.put("token", token);

    log.info("[HandshakeInterceptor] 핸드셰이크 성공 - userId={}", userId);
    return true;
  }

  private String extractTokenFromAuthorizationHeader(ServerHttpRequest request) {
    List<String> authHeaders = request.getHeaders().get("Authorization");
    if (authHeaders != null) {
      for (String header : authHeaders) {
        if (header.toLowerCase().startsWith("bearer ")) {
          return header.substring(7);
        }
      }
    }
    return null;
  }

  @Override
  public void afterHandshake(ServerHttpRequest request,
                             ServerHttpResponse response,
                             WebSocketHandler wsHandler,
                             Exception exception) {
    if (exception != null) {
      log.warn("[HandshakeInterceptor] 핸드셰이크 후 예외: {}", exception.getMessage());
    }
  }

  private String extractTokenFromQuery(URI uri) {
    String query = uri.getQuery();
    if (query != null) {
      String[] params = query.split("&");
      for (String param : params) {
        if (param.startsWith("token=")) {
          try {
            return URLDecoder.decode(param.substring(6), StandardCharsets.UTF_8);
          } catch (Exception e) {
            return param.substring(6);
          }
        }
      }
    }
    return null;
  }

  private String extractTokenFromCookies(ServerHttpRequest request) {
    List<String> cookies = request.getHeaders().get("Cookie");
    if (cookies != null) {
      for (String cookie : cookies) {
        for (String c : cookie.split(";")) {
          String[] keyValue = c.trim().split("=");
          if (keyValue.length == 2 && keyValue[0].equals("refresh_token")) {
            try {
              return URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
            } catch (Exception e) {
              return keyValue[1];
            }
          }
        }
      }
    }
    return null;
  }
}
package com.team1.otvoo.interceptor;

import com.team1.otvoo.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

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
            System.err.println("[WebSocket] ❌ 토큰이 없음: " + request.getURI());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            if (!jwtTokenProvider.validateToken(token)) {
                System.err.println("[WebSocket] ❌ 토큰 검증 실패 (만료/위조): " + token);
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                response.getHeaders().add("X-Auth-Error", "TOKEN_INVALID");
                return false;
            }
        } catch (Exception e) {
            System.err.println("[WebSocket] ❌ 토큰 검증 중 오류: " + e.getMessage());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getHeaders().add("X-Auth-Error", "TOKEN_ERROR");
            return false;
        }

        String username = jwtTokenProvider.getUsernameFromToken(token);
        attributes.put("username", username);
        attributes.put("token", token);

        System.out.println("[WebSocket] ✅ 핸드셰이크 성공 - 사용자: " + username);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        if (exception != null) {
            System.err.println("[WebSocket] ⚠ 핸드셰이크 후 예외 발생: " + exception.getMessage());
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
}

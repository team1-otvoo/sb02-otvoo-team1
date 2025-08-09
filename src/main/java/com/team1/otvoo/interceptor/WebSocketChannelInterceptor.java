package com.team1.otvoo.interceptor;

import com.team1.otvoo.directmessage.util.DmKeyUtil;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class WebSocketChannelInterceptor implements ChannelInterceptor {

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor == null) {
      log.warn("[ChannelInterceptor] StompHeaderAccessor null");
      return message;
    }

    StompCommand command = accessor.getCommand();
    if (command == null) {
      log.warn("[ChannelInterceptor] StompCommand null");
      return message;
    }

    try {
      switch (command) {
        case CONNECT:
          log.info("[ChannelInterceptor] CONNECT 시도");
          return handleConnect(accessor, message);
        case SUBSCRIBE:
          log.info("[ChannelInterceptor] SUBSCRIBE 시도, destination={}", accessor.getDestination());
          return handleSubscribe(accessor, message);
        case SEND:
          log.info("[ChannelInterceptor] SEND 시도, destination={}", accessor.getDestination());
          return handleSend(accessor, message);
        case DISCONNECT:
          log.info("[ChannelInterceptor] DISCONNECT 발생");
          handleDisconnect(accessor);
          break;
        default:
          log.info("[ChannelInterceptor] 기타 STOMP 명령: {}", command);
          break;
      }
    } catch (RestException e) {
      log.warn("[ChannelInterceptor] 보안 예외 발생: {}", e.getMessage());
      return null;
    }

    return message;
  }

  private Message<?> handleConnect(StompHeaderAccessor accessor, Message<?> message) {
    UUID userId = getUserIdFromSession(accessor);
    if (userId == null) {
      throw new RestException(ErrorCode.UNAUTHORIZED);
    }
    log.info("[ChannelInterceptor] CONNECT 인증 성공: userId={}", userId);
    return message;
  }

  private Message<?> handleSubscribe(StompHeaderAccessor accessor, Message<?> message) {
    UUID userId = getUserIdFromSession(accessor);
    String destination = accessor.getDestination();

    if (userId == null) {
      throw new RestException(ErrorCode.UNAUTHORIZED);
    }

    if (destination != null && destination.startsWith("/sub/direct-messages_")) {
      String dmKey = extractDmKey(destination);
      log.info("[ChannelInterceptor] SUBSCRIBE 요청 dmKey={}, userId={}", dmKey, userId);

      if (dmKey == null) {
        throw new RestException(ErrorCode.INVALID_INPUT_VALUE, Map.of("message", "DM Key 추출 실패"));
      }

      try {
        UUID[] users = DmKeyUtil.parse(dmKey);
        String normalizedDmKey = DmKeyUtil.generate(users[0], users[1]);

        if (!(users[0].equals(userId) || users[1].equals(userId))) {
          throw new RestException(ErrorCode.ACCESS_DENIED);
        }

        accessor.setDestination("/sub/direct-messages_" + normalizedDmKey);
        log.info("[ChannelInterceptor] destination 변경: {}", accessor.getDestination());

      } catch (Exception e) {
        throw new RestException(ErrorCode.INVALID_INPUT_VALUE, Map.of("message", "DM Key 형식 오류"));
      }
    }

    log.info("[ChannelInterceptor] 구독 요청 확인: userId={} -> destination={}", userId, accessor.getDestination());
    return message;
  }

  private Message<?> handleSend(StompHeaderAccessor accessor, Message<?> message) {
    UUID userId = getUserIdFromSession(accessor);
    String destination = accessor.getDestination();

    if (userId == null) {
      throw new RestException(ErrorCode.UNAUTHORIZED);
    }
    log.info("[ChannelInterceptor] 메시지 전송 확인: userId={} -> destination={}", userId, destination);
    return message;
  }

  private void handleDisconnect(StompHeaderAccessor accessor) {
    UUID userId = getUserIdFromSession(accessor);
    log.info("[ChannelInterceptor] STOMP 연결 해제: userId={}", userId);
  }

  private UUID getUserIdFromSession(StompHeaderAccessor accessor) {
    if (accessor.getSessionAttributes() == null) {
      log.warn("[ChannelInterceptor] 세션 속성 없음");
      return null;
    }
    Object userIdObj = accessor.getSessionAttributes().get("userId");
    if (!(userIdObj instanceof UUID)) {
      log.warn("[ChannelInterceptor] 세션 userId가 UUID 아님: {}", userIdObj);
      return null;
    }
    return (UUID) userIdObj;
  }

  private String extractDmKey(String destination) {
    if (destination == null) {
      log.warn("[ChannelInterceptor] destination null");
      return null;
    }
    int idx = destination.lastIndexOf("direct-messages_");
    if (idx == -1) {
      log.warn("[ChannelInterceptor] destination에 direct-messages_ 없음");
      return null;
    }
    String key = destination.substring(idx + "direct-messages_".length());
    log.info("[ChannelInterceptor] extractDmKey 결과: {}", key);
    return key;
  }
}
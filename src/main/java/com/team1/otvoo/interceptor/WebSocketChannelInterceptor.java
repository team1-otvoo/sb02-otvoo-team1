package com.team1.otvoo.interceptor;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class WebSocketChannelInterceptor implements ChannelInterceptor {

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    if (accessor != null) {
      StompCommand command = accessor.getCommand();

      if (command == null) return message;

      try {
        switch (command) {
          case CONNECT:
            return handleConnect(accessor, message);
          case SUBSCRIBE:
            return handleSubscribe(accessor, message);
          case SEND:
            return handleSend(accessor, message);
          case DISCONNECT:
            handleDisconnect(accessor);
            break;
          default:
            return message;
        }
      } catch (SecurityException e) {
        log.error("❌ 보안 위반 - 메시지 차단: {}", e.getMessage());
        return null;
      }
    }

    return message;
  }

  private Message<?> handleConnect(StompHeaderAccessor accessor, Message<?> message) {
    String username = (String) accessor.getSessionAttributes().get("username");
    if (username == null) {
      throw new SecurityException("인증되지 않은 연결 시도");
    }

    log.info("✅ STOMP CONNECT 인증 성공: {}", username);
    return message;
  }

  private Message<?> handleSubscribe(StompHeaderAccessor accessor, Message<?> message) {
    String destination = accessor.getDestination();
    String username = (String) accessor.getSessionAttributes().get("username");

    if (destination != null && destination.startsWith("/sub/direct-messages_")) {
      String dmKey = extractDmKey(destination);
      if (dmKey == null || !dmKey.contains(username)) {
        throw new SecurityException("DM 구독 대상에 자신이 포함되지 않음");
      }
    }

    log.info("✅ 구독 요청 확인: {} -> {}", username, destination);
    return message;
  }

  private Message<?> handleSend(StompHeaderAccessor accessor, Message<?> message) {
    String destination = accessor.getDestination();
    String username = (String) accessor.getSessionAttributes().get("username");

    if (destination == null) return message;

    if (destination.startsWith("/pub/direct-messages_send")) {
      if (username == null || username.trim().isEmpty()) {
        throw new SecurityException("인증되지 않은 사용자");
      }
    }

    log.info("✅ 메시지 전송 확인: {} -> {}", username, destination);
    return message;
  }

  private void handleDisconnect(StompHeaderAccessor accessor) {
    String username = (String) accessor.getSessionAttributes().get("username");
    log.info("⚠ STOMP 연결 해제: {}", username);
  }

  private String extractDmKey(String destination) {
    if (destination == null) return null;

    int idx = destination.lastIndexOf("direct-messages_");
    if (idx == -1) return null;

    return destination.substring(idx + "direct-messages_".length());
  }
}

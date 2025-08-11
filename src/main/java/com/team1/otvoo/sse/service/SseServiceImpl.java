package com.team1.otvoo.sse.service;

import com.team1.otvoo.sse.model.SseEmitterWrapper;
import com.team1.otvoo.sse.model.SseMessage;
import com.team1.otvoo.sse.repository.SseEmitterRepository;
import com.team1.otvoo.sse.repository.SseEventRepository;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RequiredArgsConstructor
@Service
public class SseServiceImpl implements SseService {

  @Value("${sse.timeout}")
  private long timeout;

  private final SseEmitterRepository sseEmitterRepository;
  private final SseEventRepository sseEventRepository;

  @Override
  public SseEmitter connect(UUID userId, UUID lastEventId) {
    SseEmitter sseEmitter = new SseEmitter(timeout);
    SseEmitterWrapper wrapper = SseEmitterWrapper.wrap(sseEmitter);

    // 콜백 등록(완료, 타임아웃, 에러 발생 시): 해당 Emitter 삭제
    sseEmitter.onCompletion(() -> {
      log.debug("sse on onCompletion");
      sseEmitterRepository.delete(userId, wrapper);
    });
    sseEmitter.onTimeout(() -> {
      log.debug("sse on onTimeout");
      sseEmitterRepository.delete(userId, wrapper);
    });
    sseEmitter.onError((ex) -> {
      log.debug("sse on onError");
      sseEmitterRepository.delete(userId, wrapper);
    });

    sseEmitterRepository.save(userId, wrapper);

    // 초기 연결 확인용 더미 이벤트 전송
    try {
      sseEmitter.send(SseEmitter.event()
          .name("connect")
          .data("Connected successfully")
          .build());
      log.debug("SSE 연결 완료: userId={}", userId);
    } catch (IOException e) {
      log.error("SSE 초기 연결 실패: userId={}", userId, e);
      sseEmitterRepository.delete(userId, wrapper);
      throw new RuntimeException("SSE 연결 실패", e);
    }

    // 유실된 이벤트 재전송
    Optional.ofNullable(lastEventId)
        .ifPresent(id -> {
          sseEventRepository.findAllByEventIdAfterAndReceiverId(id, userId)
              .forEach(message -> sendToEmitter(userId, wrapper, message));
        });

    return sseEmitter;
  }

  @Override
  public void sendEvent(SseMessage sseMessage) {
    // 특정 사용자에게만 보내야 하는 경우
    if (!sseMessage.isBroadcast()) {
      sseMessage.getReceiverIds().forEach(receiverId -> {
        // 이 서버에 연결된 SseEmitter가 있는지 확인
        List<SseEmitterWrapper> wrappers = sseEmitterRepository.findAllByReceiverId(receiverId);
        wrappers.forEach(wrapper -> sendToEmitter(receiverId, wrapper, sseMessage));
      });
    } else { // 브로드캐스트인 경우
      // 이 서버에 연결된 모든 Emitter에 전송
      sseEmitterRepository.forEach((receiverId, wrapper) -> {
        sendToEmitter(receiverId, wrapper, sseMessage);
      });

    }
  }

  private void sendToEmitter(UUID receiverId, SseEmitterWrapper wrapper, SseMessage message) {
    try {
      wrapper.getEmitter().send(message.toSseEventBuilder());
    } catch (IOException e) {
      log.error("SSE 이벤트 전송 중 오류 발생: receiverId={}, emitterId={}", receiverId, wrapper.getEmitterId(), e);
      // SseEmitter 상태를 '완료'로 변경 후 onError 콜백 실행: sseEmitterRepository.delete를 처리하도록 위임
      wrapper.getEmitter().completeWithError(e);
    }
  }

  @Scheduled(fixedDelay = 300000)
  public void cleanupInactiveConnections() {
    log.debug("비활성 SSE 연결 정리 작업 시작");
    SseEmitter.SseEventBuilder ping = SseEmitter.event()
        .name("ping")
        .data("heartbeat");

    sseEmitterRepository.forEach((userId, wrapper) -> {
      try {
        wrapper.getEmitter().send(ping);
      } catch (IOException e) {
        log.warn("Heartbeat 전송 실패 → emitter 종료: userId={}", userId);
        // SseEmitter 상태를 '완료'로 변경 후 onError 콜백 실행: sseEmitterRepository.delete를 처리하도록 위임
        wrapper.getEmitter().completeWithError(e);
      }
    });
  }

}
package com.team1.otvoo.sse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.team1.otvoo.sse.event.RedisStreamService;
import com.team1.otvoo.sse.model.SseEmitterWrapper;
import com.team1.otvoo.sse.model.SseMessage;
import com.team1.otvoo.sse.repository.SseEmitterRepository;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class SseServiceTest {

  @Mock
  private SseEmitterRepository emitterRepository;

  @Mock
  private RedisStreamService redisStreamService;

  private SseServiceImpl sseService;

  @BeforeEach
  void setUp() {
    sseService = new SseServiceImpl(emitterRepository, redisStreamService);
    ReflectionTestUtils.setField(sseService, "timeout", 300_000L);
  }

  @Test
  @DisplayName("SSE 연결_성공_LastEventId가 존재하지 않는 초기 연결일 경우 더미 이벤트 전송")
  void connect_Success_WithoutLastEventId_ShouldSaveEmitterAndSendDummyEvent() {
    // given
    UUID userId = UUID.randomUUID();
    SseEmitter spyEmitter = spy(new SseEmitter());
    ArgumentCaptor<SseEmitterWrapper> captor = ArgumentCaptor.forClass(SseEmitterWrapper.class);

    willAnswer(invocation -> {
      return SseEmitterWrapper.wrap(spyEmitter);
    }).given(emitterRepository).save(eq(userId), captor.capture());

    // when
    sseService.connect(userId, null);

    // then
    then(emitterRepository).should().save(eq(userId), any(SseEmitterWrapper.class));
    then(redisStreamService).shouldHaveNoMoreInteractions();

    SseEmitterWrapper savedWrapper = captor.getValue();
    assertThat(savedWrapper).isNotNull();
    // 만들어진 SseEmitter 인스턴스는 직접 검증 X, 필요하면 SseEmitter 생성부를 팩토리로 분리하는 로직 추가
    assertThat(savedWrapper.getEmitter()).isNotNull();
  }

  @Test
  @DisplayName("SSE 연결_성공_LastEventId가 존재하면 유실된 이벤트 전송")
  void connect_Success_WithLastEventId_ShouldSaveEmitterAndSendLostEvents() {
    // given
    UUID userId = UUID.randomUUID();
    UUID lastEventId = UUID.randomUUID();

    SseMessage lostMessage = SseMessage.builder()
        .eventId(UUID.randomUUID())
        .receiverIds(Set.of(userId))
        .broadcast(false)
        .eventName("notification")
        .eventData("lostNotificationInfo")
        .build();

    given(redisStreamService.findAllByEventIdAfterAndReceiverId(lastEventId, userId))
        .willReturn(List.of(lostMessage));

    SseEmitter spyEmitter = spy(new SseEmitter());
    ArgumentCaptor<SseEmitterWrapper> captor = ArgumentCaptor.forClass(SseEmitterWrapper.class);

    willAnswer(invocation -> {
      return SseEmitterWrapper.wrap(spyEmitter);
    }).given(emitterRepository).save(eq(userId), captor.capture());

    // when
    sseService.connect(userId, lastEventId);

    // then
    then(emitterRepository).should().save(eq(userId), any(SseEmitterWrapper.class));
    then(redisStreamService).should().findAllByEventIdAfterAndReceiverId(lastEventId, userId);

    SseEmitterWrapper savedWrapper = captor.getValue();
    assertThat(savedWrapper).isNotNull();
    // 만들어진 SseEmitter 인스턴스는 직접 검증 X, 필요하면 SseEmitter 생성부를 팩토리로 분리하는 로직 추가
    assertThat(savedWrapper.getEmitter()).isNotNull();
  }

  @Test
  @DisplayName("SSE 이벤트 전송_성공_특정 유저에게 전송(non-broadcast)")
  void sendEvent_Success_WhenNonBroadcast() throws Exception {
    // given
    UUID receiver = UUID.randomUUID();
    SseEmitter emitter = mock(SseEmitter.class);
    SseEmitterWrapper wrapper = SseEmitterWrapper.wrap(emitter);

    SseMessage message = SseMessage.builder()
        .eventId(UUID.randomUUID())
        .receiverIds(Set.of(receiver))
        .broadcast(false)
        .eventName("notification")
        .eventData("notificationInfo")
        .build();

    given(emitterRepository.findAllByReceiverId(receiver)).willReturn(List.of(wrapper));

    // when
    sseService.sendEvent(message);

    // then
    then(emitter).should().send(any(SseEmitter.SseEventBuilder.class));
  }


  @Test
  @DisplayName("SSE 이벤트 전송_성공_모든 유저에게 전송(broadcast)")
  void sendEvent_Success_WhenBroadcast() throws IOException {
    // given
    UUID userId = UUID.randomUUID();
    SseEmitter emitter = mock(SseEmitter.class);
    SseEmitterWrapper wrapper = SseEmitterWrapper.wrap(emitter);

    willAnswer(invocation -> {
      BiConsumer<UUID, SseEmitterWrapper> action = invocation.getArgument(0);
      action.accept(userId, wrapper);
      return null;
    }).given(emitterRepository).forEach(any());

    SseMessage message = SseMessage.builder()
        .eventId(UUID.randomUUID())
        .broadcast(true)
        .eventName("notification")
        .eventData("notificationInfo")
        .build();

    // when
    sseService.sendEvent(message);

    // then
    then(emitter).should().send(any(SseEmitter.SseEventBuilder.class));
  }
}

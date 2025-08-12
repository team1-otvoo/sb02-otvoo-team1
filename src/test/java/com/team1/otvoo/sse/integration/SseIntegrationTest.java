//package com.team1.otvoo.sse.integration;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.awaitility.Awaitility.await;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.doAnswer;
//import static org.mockito.Mockito.doThrow;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//import com.team1.otvoo.security.CustomUserDetails;
//import com.team1.otvoo.sse.event.RedisPublisher;
//import com.team1.otvoo.sse.model.SseEmitterWrapper;
//import com.team1.otvoo.sse.model.SseMessage;
//import com.team1.otvoo.sse.repository.SseEmitterRepository;
//import com.team1.otvoo.sse.repository.SseEventRepository;
//import com.team1.otvoo.sse.service.SseServiceImpl;
//import com.team1.otvoo.user.entity.User;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Set;
//import java.util.UUID;
//import java.util.concurrent.CopyOnWriteArrayList;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestInstance;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.MvcResult;
//import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
//import org.testcontainers.containers.GenericContainer;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//import org.testcontainers.utility.DockerImageName;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//@Testcontainers
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//class SseIntegrationTest {
//
//  @Container
//  @ServiceConnection
//  static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
//      .withExposedPorts(6379);
//
//  @DynamicPropertySource
//  static void properties(DynamicPropertyRegistry registry) {
////    registry.add("spring.redis.host", redis::getHost);
////    registry.add("spring.redis.port", redis::getFirstMappedPort);
//    registry.add("sse.timeout", () -> 300_000L);
//    registry.add("sse.event-ttl-seconds", () -> 300L);
//  }
//
//  @Autowired
//  private MockMvc mockMvc;
//
//  @Autowired
//  private RedisTemplate<String, Object> redisTemplate;
//
//  @Autowired
//  private RedisPublisher redisPublisher;
//
//  @Autowired
//  private SseServiceImpl sseService;
//
//  @Autowired
//  private SseEmitterRepository sseEmitterRepository;
//
//  @Autowired
//  private SseEventRepository sseEventRepository;
//
//  private UUID testUserId;
//  private CustomUserDetails testUserDetails;
//
//  @BeforeEach
//  void setUp() {
//    testUserId = UUID.randomUUID();
//    testUserDetails = createMockUserDetails(testUserId);
//
//    // Redis 초기화
//    redisTemplate.getConnectionFactory().getConnection().flushAll();
//  }
//
//  @AfterEach
//  void tearDown() {
//    // 모든 Emitter 정리
//    sseEmitterRepository.forEach((userId, wrapper) -> {
//      wrapper.getEmitter().complete();
//    });
//  }
//
//  @Test
//  @DisplayName("SSE 연결 및 초기 이벤트 전송")
//  void SseConnection_SendConnectEvent() throws Exception {
//    // when
//    MvcResult result = mockMvc.perform(get("/api/sse")
//            .with(user(testUserDetails))
//            .accept(MediaType.TEXT_EVENT_STREAM))
//        .andExpect(request().asyncStarted())
//        .andReturn();
//
//    // then
//    mockMvc.perform(asyncDispatch(result))
//        .andExpect(status().isOk())
//        .andExpect(content().contentType(MediaType.TEXT_EVENT_STREAM));
//
//    // 연결 확인 메시지 검증
//    String responseContent = result.getResponse().getContentAsString();
//    assertThat(responseContent).contains("event:connect");
//    assertThat(responseContent).contains("data:Connected successfully");
//  }
//
//  @Test
//  @DisplayName("Redis Pub/Sub를 통해 특정 사용자에게 이벤트 전송")
//  void redisPubSub_sendEvent() throws Exception {
//    // given: SSE 연결
//    MvcResult result = mockMvc.perform(get("/api/sse")
//            .with(user(testUserDetails))
//            .accept(MediaType.TEXT_EVENT_STREAM))
//        .andExpect(request().asyncStarted())
//        .andReturn();
//
//    // when: Redis로 메시지 발행
//    SseMessage message = SseMessage.builder()
//        .eventId(UUID.randomUUID())
//        .receiverIds(Set.of(testUserId))
//        .eventName("test-event")
//        .eventData("test data from redis")
//        .build();
//    redisPublisher.publish(message);
//
//    // then: 클라이언트가 해당 이벤트를 수신했는지 확인 (Awaitility 사용)
//    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
//      String responseContent = result.getResponse().getContentAsString();
//      assertThat(responseContent).contains("id:" + message.getEventId());
//      assertThat(responseContent).contains("event:test-event");
//      assertThat(responseContent).contains("data:test data from redis");
//    });
//  }
//
//  @Test
//  @DisplayName("SSE 연결 해제 시 콜백 함수 동작")
//  void sseDisconnection_OnCompletion() throws Exception {
//    // given
//    SseEmitter emitter = sseService.connect(testUserId, null);
//
//    // Emitter가 저장되었는지 확인
//    List<SseEmitterWrapper> wrappers = sseEmitterRepository.findAllByReceiverId(testUserId);
//    assertThat(wrappers).hasSize(1);
//
//    // when - completion 콜백 트리거
//    emitter.complete();
//
//    // then - Repository에서 삭제되었는지 확인
//    Thread.sleep(100); // 콜백 실행 대기
//    wrappers = sseEmitterRepository.findAllByReceiverId(testUserId);
//    assertThat(wrappers).isEmpty();
//  }
//
//  @Test
//  @DisplayName("SSE 타임아웃 시 콜백 함수 동작")
//  void sseTimeout_OnTimeout() throws Exception {
//    // given: 타임아웃이 짧은 Emitter 생성
//    SseEmitter emitter = sseService.connect(testUserId, null);
//
//    // when & then: 설정된 타임아웃(3초) 이후 Emitter가 삭제되는지 확인
//    await().atMost(5, TimeUnit.SECONDS).until(() ->
//        sseEmitterRepository.findAllByReceiverId(testUserId).isEmpty()
//    );
//  }
//
//  @Test
//  @DisplayName("SSE 에러 콜백 테스트")
//  void sseError_OnError() throws Exception {
//    // given: SseService의 sendToEmitter 메소드를 스파이하여 오류를 강제 발생
//    doAnswer(invocation -> {
//      SseMessage message = invocation.getArgument(0);
//      // 특정 사용자에게만 보내는 경우
//      if (!message.isBroadcast()) {
//        message.getReceiverIds().forEach(receiverId -> {
//          List<SseEmitterWrapper> wrappers = sseEmitterRepository.findAllByReceiverId(receiverId);
//          // 실제 send 대신 에러를 발생시키는 로직
//          wrappers.forEach(wrapper -> {
//            wrapper.getEmitter().completeWithError(new IOException("Simulated network error"));
//          });
//        });
//      }
//      return null;
//    }).when(sseService).sendEvent(any(SseMessage.class));
//
//    // SSE 연결
//    sseService.connect(testUserId, null);
//    assertThat(sseEmitterRepository.findAllByReceiverId(testUserId)).hasSize(1);
//
//    // when: 메시지 전송 (이때 스파이한 sendEvent가 호출되어 에러를 발생시킴)
//    SseMessage message = SseMessage.builder()
//        .receiverIds(Set.of(testUserId))
//        .eventName("event-that-will-fail")
//        .eventData("some data")
//        .build();
//    sseService.sendEvent(message);
//
//    // then: onError 콜백이 실행되어 Repository에서 Emitter가 삭제되었는지 확인
//    await().atMost(1, TimeUnit.SECONDS).until(() ->
//        sseEmitterRepository.findAllByReceiverId(testUserId).isEmpty()
//    );
//  }
//
//  @Test
//  @DisplayName("유실된 이벤트 재전송")
//  void missedEventResend() throws Exception {
//    // given: 유실된 이벤트들을 미리 Redis에 저장
//    UUID lastEventId = UUID.randomUUID();
//
//    SseMessage lastEvent = createTestMessage(testUserId, "last-event");
//    SseMessage missedEvent1 = createTestMessage(testUserId, "missed-event-1");
//    SseMessage missedEvent2 = createTestMessage(null, "broadcast-event", true);
//    SseMessage otherUserEvent = createTestMessage(UUID.randomUUID(), "other-user-event"); // 다른 유저의 이벤트는 무시되어야 함
//
//    // 시간 순서대로 저장
//    sseEventRepository.save(lastEvent);
//    Thread.sleep(10);
//    sseEventRepository.save(otherUserEvent);
//    Thread.sleep(10);
//    sseEventRepository.save(missedEvent1);
//    Thread.sleep(10);
//    sseEventRepository.save(missedEvent2);
//
//    // when: lastEventId를 포함하여 연결
//    MvcResult result = mockMvc.perform(get("/api/sse")
//            .param("LastEventId", lastEventId.toString())
//            .with(user(testUserDetails))
//            .accept(MediaType.TEXT_EVENT_STREAM))
//        .andExpect(request().asyncStarted())
//        .andReturn();
//
//    // then: 유실된 이벤트들이 재전송되었는지 확인
//    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
//      String responseContent = result.getResponse().getContentAsString();
//      System.out.println(responseContent); // 디버깅용
//      assertThat(responseContent).contains("id:" + missedEvent1.getEventId());
//      assertThat(responseContent).contains("event:missed-event-1");
//      assertThat(responseContent).contains("id:" + missedEvent2.getEventId());
//      assertThat(responseContent).contains("event:broadcast-event");
//      assertThat(responseContent).doesNotContain("other-user-event");
//    });
//  }
//
//  @Test
//  @DisplayName("Heartbeat(Ping) 전송 및 비정상 연결 정리")
//  void heartbeat_CleanupInactiveConnections() throws Exception {
//    // given: 정상 연결 1개, 비정상(IOException 발생) 연결 1개
//    sseService.connect(testUserId, null); // 정상 연결
//
//    UUID brokenUserId = UUID.randomUUID();
//    SseEmitter mockEmitter = mock(SseEmitter.class);
//    doThrow(new IOException("Connection lost")).when(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));
//    SseEmitterWrapper wrapper = SseEmitterWrapper.wrap(mockEmitter);
//    sseEmitterRepository.save(brokenUserId, wrapper);
//
//    doAnswer(invocation -> {
//      sseEmitterRepository.delete(brokenUserId, wrapper);
//      return null;
//    }).when(mockEmitter).completeWithError(any(Throwable.class));
//
//    assertThat(sseEmitterRepository.findAllByReceiverId(testUserId)).hasSize(1);
//    assertThat(sseEmitterRepository.findAllByReceiverId(brokenUserId)).hasSize(1);
//
//    // when: 하트비트 스케줄러 직접 호출
//    sseService.cleanupInactiveConnections();
//
//    // then: 비정상 연결만 삭제되었는지 확인
//    await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
//      assertThat(sseEmitterRepository.findAllByReceiverId(testUserId)).hasSize(1); // 정상 연결은 유지
//      assertThat(sseEmitterRepository.findAllByReceiverId(brokenUserId)).isEmpty(); // 비정상 연결은 삭제
//    });
//  }
//
//  /***********************
//   * Edge Case Test
//   ***********************/
//  @Test
//  @DisplayName("동시 다중 연결 처리")
//  void concurrentConnections() throws Exception {
//    // given
//    int connectionCount = 10;
//    ExecutorService executor = Executors.newFixedThreadPool(connectionCount);
//    // 각 클라이언트의 MvcResult를 저장할 리스트
//    List<MvcResult> results = new CopyOnWriteArrayList<>();
//    List<CustomUserDetails> users = new ArrayList<>();
//
//    for (int i = 0; i < connectionCount; i++) {
//      UUID userId = UUID.randomUUID();
//      users.add(createMockUserDetails(userId));
//    }
//
//    // when: 동시에 여러 연결 생성
//    CountDownLatch connectLatch = new CountDownLatch(connectionCount);
//    for (CustomUserDetails userDetails : users) {
//      executor.submit(() -> {
//        try {
//          MvcResult result = mockMvc.perform(get("/api/sse")
//                  .with(user(userDetails))
//                  .accept(MediaType.TEXT_EVENT_STREAM))
//              .andExpect(request().asyncStarted())
//              .andReturn();
//          results.add(result);
//        } catch (Exception e) {
//          e.printStackTrace();
//        } finally {
//          connectLatch.countDown();
//        }
//      });
//    }
//
//    // 모든 연결이 완료될 때까지 대기
//    boolean allConnected = connectLatch.await(10, TimeUnit.SECONDS);
//    assertThat(allConnected).isTrue();
//    assertThat(results).hasSize(connectionCount);
//
//    // 브로드캐스트 메시지 전송
//    SseMessage broadcastMessage = SseMessage.builder()
//        .eventId(UUID.randomUUID())
//        .broadcast(true)
//        .eventName("stress-test")
//        .eventData("Concurrent message")
//        .build();
//    redisPublisher.publish(broadcastMessage);
//
//    // then: 모든 클라이언트가 메시지를 수신했는지 검증
//    await().atMost(5, TimeUnit.SECONDS).until(() -> {
//      // 모든 MvcResult의 응답 본문에 broadcastMessage의 내용이 포함되었는지 확인
//      return results.stream().allMatch(result -> {
//        try {
//          String content = result.getResponse().getContentAsString();
//          return content.contains("id:" + broadcastMessage.getEventId()) &&
//              content.contains("data:Concurrent message");
//        } catch (Exception e) {
//          return false;
//        }
//      });
//    });
//
//    executor.shutdown();
//  }
//
//  @Test
//  @DisplayName("다중 서버 환경 시뮬레이션_Redis Pub/Sub 기반")
//  void multiServerEnvironment() throws Exception {
//    // given: Server 1에서 SSE 연결을 맺음
//    MvcResult result = mockMvc.perform(get("/api/sse")
//            .with(user(testUserDetails))
//            .accept(MediaType.TEXT_EVENT_STREAM))
//        .andExpect(request().asyncStarted())
//        .andReturn();
//
//    // Server 2에서 특정 사용자에게 보내는 메시지가 발생했다고 가정
//    SseMessage messageFromOtherServer = SseMessage.builder()
//        .eventId(UUID.randomUUID())
//        .receiverIds(Set.of(testUserId))
//        .eventName("cross-server-event")
//        .eventData("Message from another server instance")
//        .build();
//
//    // when: 다른 서버가 Redis에 메시지 발행(publish)
//    redisPublisher.publish(messageFromOtherServer);
//
//    // then: Server 1에 연결된 클라이언트가 메시지를 수신했는지 확인
//    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
//      String responseContent = result.getResponse().getContentAsString();
//      assertThat(responseContent).contains("id:" + messageFromOtherServer.getEventId());
//      assertThat(responseContent).contains("event:cross-server-event");
//      assertThat(responseContent).contains("data:Message from another server instance");
//    });
//  }
//
//  @Test
//  @DisplayName("메시지 순서 보장")
//  void messageOrdering() throws Exception {
//    // given: SSE 연결
//    MvcResult result = mockMvc.perform(get("/api/sse")
//            .with(user(testUserDetails))
//            .accept(MediaType.TEXT_EVENT_STREAM))
//        .andExpect(request().asyncStarted())
//        .andReturn();
//
//    int messageCount = 5;
//    List<SseMessage> sentMessages = new ArrayList<>();
//
//    // when: 순차적으로 메시지 발행
//    for (int i = 0; i < messageCount; i++) {
//      SseMessage message = SseMessage.builder()
//          .eventId(UUID.randomUUID())
//          .receiverIds(Set.of(testUserId))
//          .eventName("ordered-event")
//          .eventData("Data " + i)
//          .build();
//      sentMessages.add(message);
//      redisPublisher.publish(message);
//      // Pub/Sub 전파 시간을 고려한 최소한의 대기
//      Thread.sleep(20);
//    }
//
//    // then: 모든 메시지가 순서대로 수신되었는지 확인
//    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
//      String responseContent = result.getResponse().getContentAsString();
//      // 모든 메시지가 수신되었는지 먼저 확인
//      for (SseMessage msg : sentMessages) {
//        assertThat(responseContent).contains("Data " + msg.getEventData().toString().charAt(5));
//      }
//
//      // 수신된 데이터의 순서가 올바른지 확인
//      long indexData0 = responseContent.indexOf("Data 0");
//      long indexData1 = responseContent.indexOf("Data 1");
//      long indexData2 = responseContent.indexOf("Data 2");
//      long indexData3 = responseContent.indexOf("Data 3");
//      long indexData4 = responseContent.indexOf("Data 4");
//
//      assertThat(indexData0).isLessThan(indexData1);
//      assertThat(indexData1).isLessThan(indexData2);
//      assertThat(indexData2).isLessThan(indexData3);
//      assertThat(indexData3).isLessThan(indexData4);
//    });
//  }
//
//  // Helper method
//  private CustomUserDetails createMockUserDetails(UUID userId) {
//    CustomUserDetails userDetails = mock(CustomUserDetails.class);
//    User user = mock(User.class);
//    when(user.getId()).thenReturn(userId);
//    when(userDetails.getUser()).thenReturn(user);
//    return userDetails;
//  }
//
//  private SseMessage createTestMessage(UUID receiverId, String eventName) {
//    return createTestMessage(receiverId, eventName, false);
//  }
//
//  private SseMessage createTestMessage(UUID receiverId, String eventName, boolean isBroadcast) {
//    SseMessage.SseMessageBuilder builder = SseMessage.builder()
//        .eventId(UUID.randomUUID())
//        .eventName(eventName)
//        .eventData("Data for " + eventName)
//        .broadcast(isBroadcast);
//
//    if (receiverId != null) {
//      builder.receiverIds(Set.of(receiverId));
//    }
//    return builder.build();
//  }
//}
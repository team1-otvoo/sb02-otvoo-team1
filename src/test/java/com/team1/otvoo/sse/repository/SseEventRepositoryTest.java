package com.team1.otvoo.sse.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

import com.team1.otvoo.sse.model.SseMessage;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SseEventRepositoryTest {

  @Mock
  private RedisTemplate<String, Object> redisTemplate;

  @Mock
  private ValueOperations<String, Object> valueOperations;

  @Mock
  private ZSetOperations<String, Object> zSetOperations;

  @InjectMocks
  private SseEventRepository repository;

  private static final String EVENT_STREAM_KEY = "sse:events";
  private static final String EVENT_DATA_KEY_PREFIX = "sse:event_data:";
  private static final long EVENT_TTL_DAYS = 7L;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(repository, "EVENT_TTL_DAYS", EVENT_TTL_DAYS);
    lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
  }

  @Test
  @DisplayName("이벤트 저장")
  void Save() {
    // given
    UUID eventId = UUID.randomUUID();
    UUID receiverId = UUID.randomUUID();
    SseMessage message = SseMessage.builder()
        .eventId(eventId)
        .receiverIds(Set.of(receiverId))
        .eventName("test-event")
        .eventData("test data")
        .build();

    // when
    repository.save(message);

    // then
    then(zSetOperations).should().add(eq(EVENT_STREAM_KEY), eq(eventId.toString()), anyDouble());
    then(valueOperations).should().set(eq(EVENT_DATA_KEY_PREFIX + eventId), eq(message), eq(EVENT_TTL_DAYS), eq(TimeUnit.DAYS));
  }

  @Test
  @DisplayName("유실된 이벤트 조회_브로드캐스트 메시지 및 수신자 필터링 포함")
  void findAllByEventIdAfterAndReceiverId_findMissedEvents() {
    // given
    UUID lastEventId = UUID.randomUUID();
    UUID receiverId = UUID.randomUUID();
    double lastEventScore = 1000.0;

    UUID eventId1 = UUID.randomUUID();
    UUID eventId2 = UUID.randomUUID();
    UUID eventId3 = UUID.randomUUID();

    SseMessage message1 = SseMessage.builder()
        .eventId(eventId1)
        .receiverIds(Set.of(receiverId))
        .eventName("event1")
        .eventData("data1")
        .build();

    SseMessage message2 = SseMessage.builder()
        .eventId(eventId2)
        .receiverIds(Set.of(UUID.randomUUID())) // 다른 수신자
        .eventName("event2")
        .eventData("data2")
        .build();

    SseMessage message3 = SseMessage.builder()
        .eventId(eventId3)
        .broadcast(true)
        .eventName("event3")
        .eventData("data3")
        .build();

    SseMessage lastMessage = SseMessage.builder()
        .eventId(lastEventId)
        .receiverIds(Set.of(receiverId))
        .eventName("last-event")
        .eventData("last-data")
        .build();

    Set<Object> ids = new LinkedHashSet<>();
    ids.add(lastEventId.toString());
    ids.add(eventId1.toString());
    ids.add(eventId2.toString());
    ids.add(eventId3.toString());

    given(zSetOperations.score(EVENT_STREAM_KEY, lastEventId.toString())).willReturn(lastEventScore);
    given(zSetOperations.rangeByScore(EVENT_STREAM_KEY, lastEventScore, Double.MAX_VALUE)).willReturn(ids);

    List<String> keys = ids.stream().map(id -> EVENT_DATA_KEY_PREFIX + id.toString()).collect(Collectors.toList());
    given(valueOperations.multiGet(keys))
        .willReturn(List.of(lastMessage, message1, message2, message3));

    // when
    List<SseMessage> result = repository.findAllByEventIdAfterAndReceiverId(lastEventId, receiverId);

    // then
    assertThat(result).hasSize(2);
    assertThat(result).containsExactlyInAnyOrder(message1, message3); // receiverId가 수신 가능한 메시지만
    assertThat(result).doesNotContain(lastMessage); // lastEventId는 제외
    assertThat(result).doesNotContain(message2); // 다른 수신자용 메시지는 제외
  }

  @Test
  @DisplayName("유실된 이벤트 조회_lastEventId의 score가 존재하지 않는 경우")
  void findAllByEventIdAfterAndReceiverId_WhenLastEventScoreNotFound() {
    // given
    UUID lastEventId = UUID.randomUUID();
    UUID receiverId = UUID.randomUUID();
    given(zSetOperations.score(EVENT_STREAM_KEY, lastEventId.toString())).willReturn(null);

    // when
    List<SseMessage> result = repository.findAllByEventIdAfterAndReceiverId(lastEventId, receiverId);

    // then
    assertThat(result).isEmpty();
    then(zSetOperations).should(never()).rangeByScore(anyString(), anyDouble(), anyDouble());
  }

  @Test
  @DisplayName("유실된 이벤트 조회_이후 이벤트가 없는 경우")
  void findAllByEventIdAfterAndReceiverId_WhenNoEventsAfter() {
    // given
    UUID lastEventId = UUID.randomUUID();
    UUID receiverId = UUID.randomUUID();
    double lastEventScore = 1000.0;

    given(zSetOperations.score(EVENT_STREAM_KEY, lastEventId.toString())).willReturn(lastEventScore);
    given(zSetOperations.rangeByScore(EVENT_STREAM_KEY, lastEventScore, Double.MAX_VALUE))
        .willReturn(Collections.emptySet());

    // when
    List<SseMessage> result = repository.findAllByEventIdAfterAndReceiverId(lastEventId, receiverId);

    // then
    assertThat(result).isEmpty();
    then(valueOperations).should(never()).multiGet(anyList());
  }

  @Test
  @DisplayName("유실된 이벤트 조회_null 데이터 필터링")
  void findAllByEventIdAfterAndReceiverId_FilterNullDataOut() {
    // given
    UUID lastEventId = UUID.randomUUID();
    UUID receiverId = UUID.randomUUID();
    double lastEventScore = 1000.0;

    UUID eventId1 = UUID.randomUUID();
    UUID eventId2 = UUID.randomUUID();

    SseMessage message1 = SseMessage.builder()
        .eventId(eventId1)
        .receiverIds(Set.of(receiverId))
        .eventName("event1")
        .eventData("data1")
        .build();

    Set<Object> ids = new LinkedHashSet<>();
    ids.add(eventId1.toString());
    ids.add(eventId2.toString());

    given(zSetOperations.score(EVENT_STREAM_KEY, lastEventId.toString())).willReturn(lastEventScore);
    given(zSetOperations.rangeByScore(EVENT_STREAM_KEY, lastEventScore, Double.MAX_VALUE)).willReturn(ids);

    List<String> keys = ids.stream().map(id -> EVENT_DATA_KEY_PREFIX + id.toString()).collect(Collectors.toList());
    // eventId2의 데이터는 null (삭제되었거나 만료됨)
    given(valueOperations.multiGet(keys)).willReturn(Arrays.asList(message1, null));

    // when
    List<SseMessage> result = repository.findAllByEventIdAfterAndReceiverId(lastEventId, receiverId);

    // then
    assertThat(result).hasSize(1).containsExactly(message1);
  }

  @Test
  @DisplayName("유실된 이벤트 조회_시간이 동일한 이벤트 처리")
  void findAllByEventIdAfterAndReceiverId_WhenFoundSameScore() {
    // given
    UUID lastEventId = UUID.randomUUID();
    UUID receiverId = UUID.randomUUID();
    double sameScore = 1000.0;

    UUID eventId1 = UUID.randomUUID();
    UUID eventId2 = UUID.randomUUID();

    SseMessage lastMessage = SseMessage.builder()
        .eventId(lastEventId)
        .receiverIds(Set.of(receiverId))
        .eventName("last-event")
        .eventData("last-data")
        .build();

    SseMessage message1 = SseMessage.builder()
        .eventId(eventId1)
        .receiverIds(Set.of(receiverId))
        .eventName("event1")
        .eventData("data1")
        .build();

    SseMessage message2 = SseMessage.builder()
        .eventId(eventId2)
        .receiverIds(Set.of(receiverId))
        .eventName("event2")
        .eventData("data2")
        .build();

    Set<Object> ids = new LinkedHashSet<>();
    ids.add(lastEventId.toString());
    ids.add(eventId1.toString());
    ids.add(eventId2.toString());

    given(zSetOperations.score(EVENT_STREAM_KEY, lastEventId.toString())).willReturn(sameScore);
    // 같은 score를 가진 이벤트들이 모두 반환됨
    given(zSetOperations.rangeByScore(EVENT_STREAM_KEY, sameScore, Double.MAX_VALUE))
        .willReturn(ids);

    List<String> keys = ids.stream().map(id -> EVENT_DATA_KEY_PREFIX + id.toString()).collect(Collectors.toList());
    given(valueOperations.multiGet(keys)).willReturn(List.of(lastMessage, message1, message2));

    // when
    List<SseMessage> result = repository.findAllByEventIdAfterAndReceiverId(lastEventId, receiverId);

    // then
    assertThat(result).hasSize(2).containsExactlyInAnyOrder(message1, message2);
    assertThat(result).doesNotContain(lastMessage); // lastEventId는 제외
  }

  @Test
  @DisplayName("유실된 이벤트 조회_특정 수신자용 메시지만 필터링")
  void findAllByEventIdAfterAndReceiverId_multipleReceiversFiltering() {
    // given
    UUID lastEventId = UUID.randomUUID();
    UUID receiverId1 = UUID.randomUUID();
    UUID receiverId2 = UUID.randomUUID();
    UUID receiverId3 = UUID.randomUUID();
    double lastEventScore = 1000.0;

    UUID eventId1 = UUID.randomUUID();
    UUID eventId2 = UUID.randomUUID();

    SseMessage message1 = SseMessage.builder()
        .eventId(eventId1)
        .receiverIds(Set.of(receiverId1, receiverId2))
        .eventName("event1")
        .eventData("data1")
        .build();

    SseMessage message2 = SseMessage.builder()
        .eventId(eventId2)
        .receiverIds(Set.of(receiverId3))
        .eventName("event2")
        .eventData("data2")
        .build();

    Set<Object> ids = new LinkedHashSet<>();
    ids.add(eventId1.toString());
    ids.add(eventId2.toString());

    given(zSetOperations.score(EVENT_STREAM_KEY, lastEventId.toString())).willReturn(lastEventScore);
    given(zSetOperations.rangeByScore(EVENT_STREAM_KEY, lastEventScore, Double.MAX_VALUE)).willReturn(ids);

    List<String> keys = ids.stream().map(id -> EVENT_DATA_KEY_PREFIX + id.toString()).collect(Collectors.toList());
    given(valueOperations.multiGet(keys)).willReturn(List.of(message1, message2));

    // when
    List<SseMessage> result = repository.findAllByEventIdAfterAndReceiverId(lastEventId, receiverId1);

    // then
    assertThat(result).hasSize(1).containsExactly(message1); // receiverId1이 수신 가능한 메시지만 필터링
  }
}
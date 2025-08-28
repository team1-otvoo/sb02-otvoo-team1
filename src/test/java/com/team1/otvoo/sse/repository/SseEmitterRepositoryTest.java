package com.team1.otvoo.sse.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import com.team1.otvoo.sse.model.SseEmitterWrapper;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class SseEmitterRepositoryTest {

  private SseEmitterRepository repository;

  @BeforeEach
  void setUp() {
    repository = new SseEmitterRepository();
    ReflectionTestUtils.setField(repository, "maxConnections", 10000);
  }

  @Test
  @DisplayName("Emitter 저장 및 조회_단일")
  void saveAndFindAllByReceiverId_one() {
    // given
    UUID userId = UUID.randomUUID();
    SseEmitterWrapper wrapper = SseEmitterWrapper.wrap(new SseEmitter());

    // when
    repository.save(userId, wrapper);
    List<SseEmitterWrapper> result = repository.findAllByReceiverId(userId);

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(wrapper);
  }

  @Test
  @DisplayName("Emitter 저장 및 조회_동일 사용자에 대한 다중 Emitter")
  void saveAndFindAllByReceiverId_multipleEmittersForSameUser() {
    // given
    UUID userId = UUID.randomUUID();
    SseEmitterWrapper wrapper1 = SseEmitterWrapper.wrap(new SseEmitter());
    SseEmitterWrapper wrapper2 = SseEmitterWrapper.wrap(new SseEmitter());
    SseEmitterWrapper wrapper3 = SseEmitterWrapper.wrap(new SseEmitter());

    // when
    repository.save(userId, wrapper1);
    repository.save(userId, wrapper2);
    repository.save(userId, wrapper3);

    // then
    List<SseEmitterWrapper> result = repository.findAllByReceiverId(userId);
    assertThat(result).hasSize(3);
    assertThat(result).containsExactlyInAnyOrder(wrapper1, wrapper2, wrapper3);
  }

  @Test
  @DisplayName("Emitter 삭제")
  void delete() {
    // given
    UUID userId = UUID.randomUUID();
    SseEmitterWrapper wrapper1 = SseEmitterWrapper.wrap(new SseEmitter());
    SseEmitterWrapper wrapper2 = SseEmitterWrapper.wrap(new SseEmitter());

    repository.save(userId, wrapper1);
    repository.save(userId, wrapper2);

    // when
    repository.delete(userId, wrapper1);

    // then
    List<SseEmitterWrapper> result = repository.findAllByReceiverId(userId);
    assertThat(result).hasSize(1);
    assertThat(result).containsExactly(wrapper2);
  }

  @Test
  @DisplayName("Emitter 삭제_마지막 Emitter 삭제 시 사용자 맵 제거 테스트")
  void delete_LastEmitter() {
    // given
    UUID userId = UUID.randomUUID();
    SseEmitterWrapper wrapper = SseEmitterWrapper.wrap(new SseEmitter());
    repository.save(userId, wrapper);

    // when
    repository.delete(userId, wrapper);

    // then
    List<SseEmitterWrapper> result = repository.findAllByReceiverId(userId);
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("사용자로 emitter 조회_존재하지 않는 사용자 조회 시 빈 리스트 반환")
  void findAllByReceiverId_NonExistentUser() {
    // given
    UUID userId = UUID.randomUUID();

    // when
    List<SseEmitterWrapper> result = repository.findAllByReceiverId(userId);

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("forEach 실행")
  void forEach() {
    // given
    UUID userId1 = UUID.randomUUID();
    UUID userId2 = UUID.randomUUID();
    SseEmitterWrapper wrapper1 = SseEmitterWrapper.wrap(new SseEmitter());
    SseEmitterWrapper wrapper2 = SseEmitterWrapper.wrap(new SseEmitter());
    SseEmitterWrapper wrapper3 = SseEmitterWrapper.wrap(new SseEmitter());

    repository.save(userId1, wrapper1);
    repository.save(userId1, wrapper2);
    repository.save(userId2, wrapper3);

    // when
    Set<String> data = ConcurrentHashMap.newKeySet();
    repository.forEach((userId, wrapper) -> data.add(userId.toString() + ":" + wrapper.getEmitterId()));

    // then
    assertThat(data).hasSize(3);
    assertThat(data).contains(userId1.toString() + ":" + wrapper1.getEmitterId());
    assertThat(data).contains(userId1.toString() + ":" + wrapper2.getEmitterId());
    assertThat(data).contains(userId2.toString() + ":" + wrapper3.getEmitterId());
  }

  /***********************
   * Edge Case Test - 보완 필요
   ***********************/
  @Test
  @DisplayName("동일한 Emitter 중복 저장 시 덮어쓰기")
  void overwriteSameEmitter() {
    // given
    UUID userId = UUID.randomUUID();
    SseEmitter sseEmitter = new SseEmitter();
    SseEmitterWrapper wrapper1 = SseEmitterWrapper.wrap(sseEmitter);

    // when
    repository.save(userId, wrapper1);
    repository.save(userId, wrapper1); // 동일한 wrapper 재저장

    // then
    List<SseEmitterWrapper> result = repository.findAllByReceiverId(userId);
    assertThat(result).hasSize(1);
  }

  @Test
  @DisplayName("동시성 테스트_멀티스레드 환경에서 저장/삭제 시 안정성 테스트")
  void testConcurrency() throws InterruptedException {
    // given
    int threadCount = 8;
    int operationsPerThread = 200;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);

    // when
    for (int i = 0; i < threadCount; i++) {
      executor.submit(() -> {
        try {
          for (int j = 0; j < operationsPerThread; j++) {
            UUID userId = UUID.randomUUID();
            SseEmitterWrapper wrapper = SseEmitterWrapper.wrap(new SseEmitter());

            // 저장
            repository.save(userId, wrapper);

            // 조회
            List<SseEmitterWrapper> found = repository.findAllByReceiverId(userId);
            assertThat(found).contains(wrapper);

            // 삭제
            if (j % 2 == 0) {
              repository.delete(userId, wrapper);
              List<SseEmitterWrapper> afterDelete = repository.findAllByReceiverId(userId);
              assertThat(afterDelete).doesNotContain(wrapper);
            }
          }
        } finally {
          latch.countDown();
        }
      });
    }

    // then
    boolean completed = latch.await(10, TimeUnit.SECONDS);
    assertThat(completed).isTrue();
    executor.shutdown();
  }

  @Test
  @DisplayName("최대 연결 수를 초과하여 Emitter 저장 시 예외 발생")
  void save_throwsExceptionWhenMaxConnectionsExceeded() {
    // given
    ReflectionTestUtils.setField(repository, "maxConnections", 1); // 최대 연결 수를 1로 제한

    UUID user1 = UUID.randomUUID();
    SseEmitterWrapper wrapper1 = SseEmitterWrapper.wrap(new SseEmitter());
    repository.save(user1, wrapper1); // 1개 연결

    // when & then
    UUID user2 = UUID.randomUUID();
    SseEmitterWrapper wrapper2 = SseEmitterWrapper.wrap(new SseEmitter());

    assertThatThrownBy(() -> repository.save(user2, wrapper2))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("최대 연결 수를 초과했습니다.");

    // 롤백이 잘 되었는지 확인 (전체 연결 수는 여전히 1이어야 함)
    AtomicInteger connectionCount = (AtomicInteger) ReflectionTestUtils.getField(repository, "connectionCount");
    assertThat(connectionCount.get()).isEqualTo(1);
  }

}
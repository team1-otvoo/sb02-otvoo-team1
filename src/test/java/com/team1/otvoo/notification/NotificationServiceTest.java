package com.team1.otvoo.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;

import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.notification.dto.NotificationDto;
import com.team1.otvoo.notification.dto.NotificationDtoCursorResponse;
import com.team1.otvoo.notification.entity.Notification;
import com.team1.otvoo.notification.entity.NotificationLevel;
import com.team1.otvoo.notification.mapper.NotificationMapper;
import com.team1.otvoo.notification.repository.NotificationRepository;
import com.team1.otvoo.notification.service.NotificationServiceImpl;
import com.team1.otvoo.user.entity.User;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

  @InjectMocks
  private NotificationServiceImpl notificationService;

  @Mock
  private NotificationRepository notificationRepository;

  @Mock
  private NotificationMapper notificationMapper;

  private UUID receiverId;
  private UUID notificationId;
  private User receiver;
  private Notification notification;
  private NotificationDto notificationDto;
  private String title;
  private String content;
  private NotificationLevel level;

  @BeforeEach
  void setUp() {
    receiverId = UUID.randomUUID();
    notificationId = UUID.randomUUID();
    title = "ㅇㅇ님이 댓글을 달았어요.";
    content = "댓글 내용";
    level = NotificationLevel.INFO;

    // receiver
    receiver = spy(new User("test@receiver.com", "password123"));
    ReflectionTestUtils.setField(receiver, "id", receiverId);

    // notification entity
    notification = new Notification(receiver, title, content, level);
    ReflectionTestUtils.setField(notification, "id", notificationId);
    ReflectionTestUtils.setField(notification, "createdAt", Instant.now());

    // dto
    notificationDto = new NotificationDto(
        notificationId,
        Instant.now(),
        receiverId,
        title,
        content,
        level
    );
  }

  @Nested
  @DisplayName("알림 목록 조회 테스트")
  class GetNoificationListTests {

    @Test
    @DisplayName("성공_다음 페이지가 있는 경우")
    void getList_Success_WhenHasNextPage() {
      // given
      int limit = 20;

      // limit+1 만큼의 Mock 데이터 생성
      List<Notification> mockNotifications = createMockNotificationList(limit + 1);
      List<NotificationDto> mockNotificationDtos = mockNotifications.stream().map(n -> notificationDto).collect(Collectors.toList());

      given(notificationRepository.findNotificationsWithCursor(any(UUID.class), any(), any(), eq(limit + 1)))
          .willReturn(mockNotifications);
      given(notificationRepository.countByReceiverId(receiverId)).willReturn(100L);
      given(notificationMapper.toDtoList(mockNotifications.subList(0, limit))).willReturn(mockNotificationDtos.subList(0, limit));

      // when
      NotificationDtoCursorResponse response = notificationService.getList(receiverId, null, null, limit);

      // then
      assertThat(response.hasNext()).isTrue(); // hasNext = true
      assertThat(response.data().size()).isEqualTo(limit);
      assertThat(response.totalCount()).isEqualTo(100L);

      // 마지막 데이터(limit-1 인덱스)로 nextCursor, nextIdAfter가 생성되었는지 확인
      Notification lastNotificationInPage = mockNotifications.get(limit - 1);
      assertThat(response.nextCursor()).isEqualTo(lastNotificationInPage.getCreatedAt().toString());
      assertThat(response.nextIdAfter()).isEqualTo(lastNotificationInPage.getId());

      then(notificationRepository).should().findNotificationsWithCursor(eq(receiverId), isNull(), isNull(), eq(limit + 1));
      then(notificationRepository).should().countByReceiverId(eq(receiverId));
      then(notificationMapper).should().toDtoList(mockNotifications.subList(0, limit));
    }

    @Test
    @DisplayName("성공_마지막 페이지인 경우")
    void getList_Success_WhenIsLastPage() {
      // given
      int limit = 20;
      int resultSize = 19;

      // resultSize 만큼의 Mock 데이터 생성
      List<Notification> mockNotifications = createMockNotificationList(resultSize);
      List<NotificationDto> mockNotificationDtos = mockNotifications.stream().map(n -> notificationDto).collect(Collectors.toList());

      given(notificationRepository.findNotificationsWithCursor(any(UUID.class), any(), any(), eq(limit + 1)))
          .willReturn(mockNotifications);
      given(notificationRepository.countByReceiverId(receiverId)).willReturn((long) resultSize);
      given(notificationMapper.toDtoList(mockNotifications)).willReturn(mockNotificationDtos);

      // when
      NotificationDtoCursorResponse response = notificationService.getList(receiverId, null, null, limit);

      // then
      assertThat(response.hasNext()).isFalse(); // hasNext = false
      assertThat(response.data().size()).isEqualTo(resultSize);
      assertThat(response.totalCount()).isEqualTo(resultSize);

      // 마지막 데이터(limit-1 인덱스)로 nextCursor, nextIdAfter가 생성되었는지 확인
      Notification lastNotificationInPage = mockNotifications.get(resultSize - 1);
      assertThat(response.nextCursor()).isEqualTo(lastNotificationInPage.getCreatedAt().toString());
      assertThat(response.nextIdAfter()).isEqualTo(lastNotificationInPage.getId());

      then(notificationRepository).should().findNotificationsWithCursor(eq(receiverId), isNull(), isNull(), eq(limit + 1));
      then(notificationRepository).should().countByReceiverId(eq(receiverId));
      then(notificationMapper).should().toDtoList(mockNotifications);
    }

    @Test
    @DisplayName("성공_결과가 없는 경우")
    void getList_Success_WhenResultIsEmpty() {
      // given
      int limit = 20;

      given(notificationRepository.findNotificationsWithCursor(any(UUID.class), any(), any(), eq(limit + 1)))
          .willReturn(Collections.emptyList());

      // when
      NotificationDtoCursorResponse response = notificationService.getList(receiverId, null, null, limit);

      // then
      assertThat(response.hasNext()).isFalse();
      assertThat(response.data()).isEmpty();
      assertThat(response.totalCount()).isEqualTo(0L);
      assertThat(response.nextCursor()).isNull();
      assertThat(response.nextIdAfter()).isNull();

      then(notificationRepository).should().findNotificationsWithCursor(eq(receiverId), isNull(), isNull(), eq(limit + 1));
      then(notificationRepository).should(never()).countByReceiverId(eq(receiverId));
      then(notificationMapper).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("실패_유효하지 않은 cursor 포맷")
    void getList_Failure_ShouldThrowException_WithInvalidCursorFormat() {
      // given
      String invalidCursor = "이건-날짜가-아닙니다";
      int limit = 20;

      // when & then
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
          () -> notificationService.getList(receiverId, invalidCursor, null, limit));

      assertThat(exception.getMessage()).contains("Invalid cursor format");

      then(notificationRepository).shouldHaveNoInteractions();
      then(notificationMapper).shouldHaveNoInteractions();
    }

  }

  @Nested
  @DisplayName("알림 읽음 처리 테스트")
  class DeleteNotificationTests {

    @Test
    @DisplayName("성공")
    void deleteNotification_Success() {
      // given
      given(notificationRepository.findById(notificationId)).willReturn(Optional.of(notification));

      // when
      notificationService.delete(notificationId);

      // then
      then(notificationRepository).should().findById(notificationId);
      then(notificationRepository).should().delete(notification);
    }

    @Test
    @DisplayName("실패_존재하지 않는 알림 내역")
    void deleteNotification_Failure_ShouldThrowException_WhenNotificationNotFound() {
      // given
      given(notificationRepository.findById(notificationId)).willReturn(Optional.empty());

      // when & then
      RestException exception = assertThrows(RestException.class,
          () -> notificationService.delete(notificationId));

      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
      assertThat(exception.getDetails().get("id")).isEqualTo(notificationId);

      // then
      then(notificationRepository).should().findById(notificationId);
      then(notificationRepository).should(never()).delete(notification);
    }

  }

  /**
   * 테스트용 Notification 엔티티 리스트 생성
   */
  private List<Notification> createMockNotificationList(int count) {
    return IntStream.range(0, count)
        .mapToObj(i -> {
          Notification n = new Notification(receiver, title, content, level);
          ReflectionTestUtils.setField(n, "id", UUID.randomUUID());
          ReflectionTestUtils.setField(n, "createdAt", Instant.now().minusSeconds(i));
          return n;
        })
        .collect(Collectors.toList());
  }
}

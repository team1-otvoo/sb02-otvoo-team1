package com.team1.otvoo.comment.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.BDDMockito.*;

import com.team1.otvoo.comment.entity.FeedComment;
import com.team1.otvoo.feed.entity.Feed;
import com.team1.otvoo.notification.service.SendNotificationService;
import com.team1.otvoo.user.entity.User;
import java.util.UUID;
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
class CommentEventHandlerTest {

  @InjectMocks
  private CommentEventHandler commentEventHandler;

  @Mock
  private SendNotificationService sendNotificationService;

  @Mock
  private User mockUser;

  @Mock
  private Feed mockFeed;

  private CommentEvent commentEvent;
  private FeedComment feedComment;

  @BeforeEach
  void setUp() {
    feedComment = spy(new FeedComment(mockUser, mockFeed, "테스트 댓글"));
    ReflectionTestUtils.setField(feedComment, "id", UUID.randomUUID());

    commentEvent = new CommentEvent(feedComment);
  }

  @Nested
  @DisplayName("댓글 이벤트 처리 테스트")
  class HandleCommentEventTests {

    @Test
    @DisplayName("성공_댓글 이벤트 발생 시 알림 서비스 호출")
    void handleEvent_Success_ShouldCallNotificationService() {
      // when
      commentEventHandler.handleEvent(commentEvent);

      // then
      then(sendNotificationService).should().sendCommentNotification(feedComment);
    }

    @Test
    @DisplayName("실패_알림 서비스에서 예외 발생 시에도 핸들러는 정상 종료")
    void handleEvent_Failure_ShouldCompleteGracefully() {
      // given
      willThrow(new RuntimeException("알림 전송 실패"))
          .given(sendNotificationService).sendCommentNotification(any(FeedComment.class));

      // when & then
      assertDoesNotThrow(() -> commentEventHandler.handleEvent(commentEvent));
      then(sendNotificationService).should().sendCommentNotification(feedComment);
    }
  }
}

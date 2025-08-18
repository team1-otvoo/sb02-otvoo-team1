package com.team1.otvoo.feed.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.BDDMockito.*;

import com.team1.otvoo.feed.entity.Feed;
import com.team1.otvoo.feed.entity.FeedLike;
import com.team1.otvoo.notification.service.SendNotificationService;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.weather.entity.WeatherForecast;
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
class FeedEventHandlerTest {

  @InjectMocks
  private FeedEventHandler feedEventHandler;

  @Mock
  private SendNotificationService sendNotificationService;

  @Mock
  private User mockUser;

  @Mock
  private WeatherForecast mockWeatherForecast;

  @Nested
  @DisplayName("피드 생성 이벤트 처리 테스트")
  class HandleFeedEventTests {

    private FeedEvent feedEvent;
    private Feed feed;

    @BeforeEach
    void setUp() {
      feed = spy(new Feed(mockUser, mockWeatherForecast, "feed content"));
      ReflectionTestUtils.setField(feed, "id", UUID.randomUUID());
      feedEvent = new FeedEvent(feed);
    }

    @Test
    @DisplayName("성공_피드 생성 이벤트 발생 시 알림 서비스 호출")
    void handleEvent_Success_ShouldCallNotificationService() {
      // when
      feedEventHandler.handleEvent(feedEvent);

      // then
      then(sendNotificationService).should().sendFeedNotification(feed);
    }

    @Test
    @DisplayName("실패_알림 서비스에서 예외 발생 시에도 핸들러는 정상 종료")
    void handleEvent_Failure_ShouldCompleteGracefully() {
      // given
      willThrow(new RuntimeException("알림 전송 실패"))
          .given(sendNotificationService).sendFeedNotification(any(Feed.class));

      // when & then
      assertDoesNotThrow(() -> feedEventHandler.handleEvent(feedEvent));
      then(sendNotificationService).should().sendFeedNotification(feed);
    }
  }

  @Nested
  @DisplayName("피드 좋아요 이벤트 처리 테스트")
  class HandleFeedLikeEventTests {

    private FeedLikeEvent feedLikeEvent;
    private FeedLike feedLike;

    @Mock
    private Feed mockFeed;

    @BeforeEach
    void setUp() {
      feedLike = spy(new FeedLike(mockFeed, mockUser));
      ReflectionTestUtils.setField(feedLike, "id", UUID.randomUUID());
      feedLikeEvent = new FeedLikeEvent(feedLike);
    }

    @Test
    @DisplayName("성공_좋아요 이벤트 발생 시 알림 서비스 호출")
    void handleLikeEvent_Success_ShouldCallLikeNotification() {
      // when
      feedEventHandler.handleLikeEvent(feedLikeEvent);

      // then
      then(sendNotificationService).should().sendLikeNotification(feedLike);
    }

    @Test
    @DisplayName("실패_알림 서비스에서 예외 발생 시에도 핸들러는 정상 종료")
    void handleLikeEvent_Failure_ShouldCompleteGracefully() {
      // given
      willThrow(new RuntimeException("알림 전송 실패"))
          .given(sendNotificationService).sendLikeNotification(any(FeedLike.class));

      // when & then
      assertDoesNotThrow(() -> feedEventHandler.handleLikeEvent(feedLikeEvent));
      then(sendNotificationService).should().sendLikeNotification(feedLike);
    }
  }
}
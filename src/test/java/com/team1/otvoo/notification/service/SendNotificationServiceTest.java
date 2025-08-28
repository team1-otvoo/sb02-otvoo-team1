package com.team1.otvoo.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.team1.otvoo.clothes.entity.ClothesAttributeDefinition;
import com.team1.otvoo.clothes.entity.ClothesAttributeValue;
import com.team1.otvoo.comment.entity.FeedComment;
import com.team1.otvoo.directmessage.entity.DirectMessage;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.feed.entity.Feed;
import com.team1.otvoo.feed.entity.FeedLike;
import com.team1.otvoo.follow.entity.Follow;
import com.team1.otvoo.follow.repository.FollowRepository;
import com.team1.otvoo.notification.dto.NotificationDto;
import com.team1.otvoo.notification.entity.Notification;
import com.team1.otvoo.notification.entity.NotificationLevel;
import com.team1.otvoo.notification.event.NotificationEvent;
import com.team1.otvoo.notification.mapper.NotificationMapper;
import com.team1.otvoo.notification.repository.NotificationRepository;
import com.team1.otvoo.user.entity.Profile;
import com.team1.otvoo.user.entity.Role;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.ProfileRepository;
import com.team1.otvoo.weather.entity.WeatherForecast;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class SendNotificationServiceTest {

  @InjectMocks
  private SendNotificationServiceImpl sendNotificationService;

  @Mock
  private NotificationRepository notificationRepository;

  @Mock
  private ProfileRepository profileRepository;

  @Mock
  private FollowRepository followRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Mock
  private NotificationMapper notificationMapper;

  private UUID userId;
  private UUID receiverId;
  private UUID followerId;
  private UUID notificationId;
  private User user;
  private User receiver;
  private User follower;
  private Profile profile;
  private Notification notification;
  private NotificationDto notificationDto;
  private Feed feed;
  private FeedLike feedLike;
  private FeedComment feedComment;
  private Follow follow;
  private DirectMessage directMessage;
  private ClothesAttributeDefinition clothesAttributeDefinition;
  private WeatherForecast weatherForecast;
  private List<ClothesAttributeValue> clothesAttributeValues;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    receiverId = UUID.randomUUID();
    followerId = UUID.randomUUID();
    notificationId = UUID.randomUUID();

    // User entities
    user = spy(new User("test@user.com", "password123"));
    ReflectionTestUtils.setField(user, "id", userId);
    ReflectionTestUtils.setField(user, "role", Role.USER);

    receiver = spy(new User("test@receiver.com", "password123"));
    ReflectionTestUtils.setField(receiver, "id", receiverId);

    follower = spy(new User("test@follower.com", "password123"));
    ReflectionTestUtils.setField(follower, "id", followerId);

    // Profile
    profile = new Profile("TestUser", user);

    // Notification
    notification = new Notification(receiver, "Test Title", "Test Content", NotificationLevel.INFO);
    ReflectionTestUtils.setField(notification, "id", notificationId);
    ReflectionTestUtils.setField(notification, "createdAt", Instant.now());

    // NotificationDto
    notificationDto = new NotificationDto(
        notificationId,
        notification.getCreatedAt(),
        receiverId,
        "Test Title",
        "Test Content",
        NotificationLevel.INFO
    );

    // Feed
    weatherForecast = mock(WeatherForecast.class);
    feed = new Feed(user, weatherForecast, "Test feed content");
    ReflectionTestUtils.setField(feed, "id", UUID.randomUUID());

    // FeedLike
    feedLike = new FeedLike(feed, follower);

    // FeedComment
    feedComment = new FeedComment(follower, feed, "Test comment");

    // Follow
    follow = new Follow(user, follower);

    // DirectMessage
    directMessage = DirectMessage.builder()
        .sender(follower)
        .receiver(receiver)
        .content("Test DM")
        .createdAt(Instant.now())
        .build();

    // ClothesAttributeDefinition
    ClothesAttributeValue mockAttributeValue = mock(ClothesAttributeValue.class);
    clothesAttributeValues = List.of(mockAttributeValue);
    clothesAttributeDefinition = new ClothesAttributeDefinition("TestAttribute", clothesAttributeValues);
  }

  @Nested
  @DisplayName("사용자 권한 변경 알림 테스트")
  class SendUserRoleNotificationTests {

    @Test
    @DisplayName("성공 - 권한 변경 알림을 정상적으로 전송한다")
    void sendUserRoleNotification_Success_ShouldSendNotificationWhenRoleChanged() {
      // given
      Role previousRole = Role.USER;
      ReflectionTestUtils.setField(user, "role", Role.ADMIN);

      given(notificationRepository.save(any(Notification.class))).willReturn(notification);
      given(notificationMapper.toDto(any(Notification.class))).willReturn(notificationDto);

      // when
      sendNotificationService.sendUserRoleNotification(previousRole, user);

      // then
      then(notificationRepository).should().save(any(Notification.class));
      then(eventPublisher).should().publishEvent(any(NotificationEvent.class));

      ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
      then(notificationRepository).should().save(notificationCaptor.capture());

      Notification savedNotification = notificationCaptor.getValue();
      assertThat(savedNotification.getReceiver()).isEqualTo(user);
      assertThat(savedNotification.getTitle()).contains("권한이 변경");
      assertThat(savedNotification.getContent()).contains(previousRole.name());
      assertThat(savedNotification.getContent()).contains(user.getRole().name());
    }
  }

  @Nested
  @DisplayName("의상 속성 변경 알림 테스트")
  class SendClothesAttributeNotificationTests {

    @Test
    @DisplayName("성공 - CREATE 메소드타입으로 의상 속성 추가 알림을 전송한다")
    void sendClothesAttributeNotification_Success_ShouldSendBroadcastWhenCreated() {
      // given
      String methodType = "CREATE";
      given(notificationRepository.save(any(Notification.class))).willReturn(notification);
      given(notificationMapper.toDto(any(Notification.class))).willReturn(notificationDto);

      // when
      sendNotificationService.sendClothesAttributeNotification(methodType, clothesAttributeDefinition);

      // then
      then(eventPublisher).should().publishEvent(any(NotificationEvent.class));
    }

    @Test
    @DisplayName("성공 - UPDATE 메소드타입으로 의상 속성 수정 알림을 전송한다")
    void sendClothesAttributeNotification_Success_ShouldSendBroadcastWhenUpdated() {
      // given
      String methodType = "UPDATE";
      given(notificationRepository.save(any(Notification.class))).willReturn(notification);
      given(notificationMapper.toDto(any(Notification.class))).willReturn(notificationDto);

      // when
      sendNotificationService.sendClothesAttributeNotification(methodType, clothesAttributeDefinition);

      // then
      then(eventPublisher).should().publishEvent(any(NotificationEvent.class));
    }
  }

  @Nested
  @DisplayName("좋아요 알림 테스트")
  class SendLikeNotificationTests {

    @Test
    @DisplayName("성공 - 좋아요 알림을 정상적으로 전송한다")
    void sendLikeNotification_Success_ShouldSendNotificationWhenFeedLiked() {
      // given
      given(profileRepository.findByUserId(followerId)).willReturn(Optional.of(profile));
      given(notificationRepository.save(any(Notification.class))).willReturn(notification);
      given(notificationMapper.toDto(any(Notification.class))).willReturn(notificationDto);

      // when
      sendNotificationService.sendLikeNotification(feedLike);

      // then
      then(profileRepository).should().findByUserId(followerId);
      then(notificationRepository).should().save(any(Notification.class));
      then(eventPublisher).should().publishEvent(any(NotificationEvent.class));

      ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
      then(notificationRepository).should().save(notificationCaptor.capture());

      Notification savedNotification = notificationCaptor.getValue();
      assertThat(savedNotification.getReceiver()).isEqualTo(user);
      assertThat(savedNotification.getTitle()).contains(profile.getName());
      assertThat(savedNotification.getTitle()).contains("TestUser님이 내 피드를 좋아합니다.");
    }

    @Test
    @DisplayName("실패 - 프로필을 찾을 수 없으면 예외를 발생시킨다")
    void sendLikeNotification_Failure_ShouldThrowExceptionWhenProfileNotFound() {
      // given
      given(profileRepository.findByUserId(followerId)).willReturn(Optional.empty());

      // when & then
      RestException exception = assertThrows(RestException.class,
          () -> sendNotificationService.sendLikeNotification(feedLike));

      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
      assertThat(exception.getDetails()).containsEntry("id", followerId);

      then(profileRepository).should().findByUserId(followerId);
      then(notificationRepository).shouldHaveNoInteractions();
      then(eventPublisher).shouldHaveNoInteractions();
    }
  }

  @Nested
  @DisplayName("댓글 알림 테스트")
  class SendCommentNotificationTests {

    @Test
    @DisplayName("성공 - 댓글 알림을 정상적으로 전송한다")
    void sendCommentNotification_Success_ShouldSendNotificationWhenCommented() {
      // given
      given(profileRepository.findByUserId(followerId)).willReturn(Optional.of(profile));
      given(notificationRepository.save(any(Notification.class))).willReturn(notification);
      given(notificationMapper.toDto(any(Notification.class))).willReturn(notificationDto);

      // when
      sendNotificationService.sendCommentNotification(feedComment);

      // then
      then(profileRepository).should().findByUserId(followerId);
      then(notificationRepository).should().save(any(Notification.class));
      then(eventPublisher).should().publishEvent(any(NotificationEvent.class));

      ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
      then(notificationRepository).should().save(notificationCaptor.capture());

      Notification savedNotification = notificationCaptor.getValue();
      assertThat(savedNotification.getReceiver()).isEqualTo(user);
      assertThat(savedNotification.getTitle()).contains(profile.getName());
      assertThat(savedNotification.getTitle()).contains("댓글");
      assertThat(savedNotification.getContent()).contains(feedComment.getContent());
    }

    @Test
    @DisplayName("실패 - 프로필을 찾을 수 없으면 예외를 발생시킨다")
    void sendCommentNotification_Failure_ShouldThrowExceptionWhenProfileNotFound() {
      // given
      given(profileRepository.findByUserId(followerId)).willReturn(Optional.empty());

      // when & then
      RestException exception = assertThrows(RestException.class,
          () -> sendNotificationService.sendCommentNotification(feedComment));

      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
      assertThat(exception.getDetails()).containsEntry("id", followerId);

      then(profileRepository).should().findByUserId(followerId);
      then(notificationRepository).shouldHaveNoInteractions();
      then(eventPublisher).shouldHaveNoInteractions();
    }
  }

  @Nested
  @DisplayName("피드 등록 알림 테스트")
  class SendFeedNotificationTests {

    @Test
    @DisplayName("성공 - 팔로워들에게 피드 등록 알림을 전송한다")
    void sendFeedNotification_Success_ShouldSendNotificationToAllFollowers() {
      User follower1 = spy(new User("f1@test.com", "pw"));
      ReflectionTestUtils.setField(follower1, "id", UUID.randomUUID());
      User follower2 = spy(new User("f2@test.com", "pw"));
      ReflectionTestUtils.setField(follower2, "id", UUID.randomUUID());

      List<User> followers = List.of(follower1, follower2);
      Page<User> followerPage = new PageImpl<>(followers);

      given(profileRepository.findByUserId(userId)).willReturn(Optional.of(profile));
      given(followRepository.findFollowersByFolloweeId(eq(userId), any(Pageable.class)))
          .willReturn(followerPage);
      given(notificationRepository.saveAll(anyList())).willReturn(List.of(notification));
      given(notificationMapper.toDto(any(Notification.class))).willReturn(notificationDto);

      // when
      sendNotificationService.sendFeedNotification(feed);

      // then
      then(notificationRepository).should().saveAll(anyList());
      then(eventPublisher).should().publishEvent(any(NotificationEvent.class));
    }

    @Test
    @DisplayName("성공 - 팔로워가 없으면 알림을 전송하지 않는다")
    void sendFeedNotification_Success_ShouldNotSendWhenNoFollowers() {
      // given
      given(profileRepository.findByUserId(userId)).willReturn(Optional.of(profile));
      given(followRepository.findFollowersByFolloweeId(eq(userId), any(Pageable.class)))
          .willReturn(Page.empty());

      // when
      sendNotificationService.sendFeedNotification(feed);

      // then
      then(profileRepository).should().findByUserId(userId);
      then(followRepository).should().findFollowersByFolloweeId(eq(userId), any(Pageable.class));
      then(notificationRepository).shouldHaveNoInteractions();
      then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("실패 - 프로필을 찾을 수 없으면 예외를 발생시킨다")
    void sendFeedNotification_Failure_ShouldThrowExceptionWhenProfileNotFound() {
      // given
      given(profileRepository.findByUserId(userId)).willReturn(Optional.empty());

      // when & then
      RestException exception = assertThrows(RestException.class,
          () -> sendNotificationService.sendFeedNotification(feed));

      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
      assertThat(exception.getDetails()).containsEntry("id", userId);

      then(profileRepository).should().findByUserId(userId);
      then(followRepository).shouldHaveNoInteractions();
      then(notificationRepository).shouldHaveNoInteractions();
      then(eventPublisher).shouldHaveNoInteractions();
    }
  }

  @Nested
  @DisplayName("팔로우 알림 테스트")
  class SendFollowNotificationTests {

    @Test
    @DisplayName("성공 - 팔로우 알림을 정상적으로 전송한다")
    void sendFollowNotification_Success_ShouldSendNotificationWhenFollowed() {
      // given
      given(profileRepository.findByUserId(followerId)).willReturn(Optional.of(profile));
      given(notificationRepository.save(any(Notification.class))).willReturn(notification);
      given(notificationMapper.toDto(any(Notification.class))).willReturn(notificationDto);

      // when
      sendNotificationService.sendFollowNotification(follower, receiver);

      // then
      then(profileRepository).should().findByUserId(followerId);
      then(notificationRepository).should().save(any(Notification.class));
      then(eventPublisher).should().publishEvent(any(NotificationEvent.class));

      ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
      then(notificationRepository).should().save(notificationCaptor.capture());

      Notification savedNotification = notificationCaptor.getValue();
      assertThat(savedNotification.getReceiver()).isEqualTo(receiver);
      assertThat(savedNotification.getTitle()).contains(profile.getName());
      assertThat(savedNotification.getTitle()).contains("팔로우");
    }

    @Test
    @DisplayName("실패 - 프로필을 찾을 수 없으면 예외를 발생시킨다")
    void sendFollowNotification_Failure_ShouldThrowExceptionWhenProfileNotFound() {
      // given
      given(profileRepository.findByUserId(followerId)).willReturn(Optional.empty());

      // when & then
      RestException exception = assertThrows(RestException.class,
          () -> sendNotificationService.sendFollowNotification(follower, receiver));

      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
      assertThat(exception.getDetails()).containsEntry("id", followerId);

      then(profileRepository).should().findByUserId(followerId);
      then(notificationRepository).shouldHaveNoInteractions();
      then(eventPublisher).shouldHaveNoInteractions();
    }
  }

  @Nested
  @DisplayName("DM 알림 테스트")
  class SendDirectMessageNotificationTests {

    @Test
    @DisplayName("성공 - DM 알림을 정상적으로 전송한다")
    void sendDirectMessageNotification_Success_ShouldSendNotificationWhenDirectMessageSent() {
      // given
      UUID senderId = directMessage.getSender().getId();
      Profile senderProfile = new Profile("SenderUser", directMessage.getSender());
      given(profileRepository.findByUserId(senderId)).willReturn(Optional.of(senderProfile));
      given(notificationRepository.save(any(Notification.class))).willReturn(notification);
      given(notificationMapper.toDto(any(Notification.class))).willReturn(notificationDto);

      // when
      sendNotificationService.sendDirectMessageNotification(directMessage);

      // then
      then(profileRepository).should().findByUserId(senderId);
      then(notificationRepository).should().save(any(Notification.class));
      then(eventPublisher).should().publishEvent(any(NotificationEvent.class));

      ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
      then(notificationRepository).should().save(notificationCaptor.capture());

      Notification savedNotification = notificationCaptor.getValue();
      assertThat(savedNotification.getReceiver()).isEqualTo(receiver);
      assertThat(savedNotification.getTitle()).contains(senderProfile.getName());
      assertThat(savedNotification.getContent()).contains(directMessage.getContent());
    }

    @Test
    @DisplayName("실패 - 프로필을 찾을 수 없으면 예외를 발생시킨다")
    void sendDirectMessageNotification_Failure_ShouldThrowExceptionWhenProfileNotFound() {
      // given
      UUID senderId = directMessage.getSender().getId();
      given(profileRepository.findByUserId(senderId)).willReturn(Optional.empty());

      // when & then
      RestException exception = assertThrows(RestException.class,
          () -> sendNotificationService.sendDirectMessageNotification(directMessage));

      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
      assertThat(exception.getDetails()).containsEntry("id", senderId);

      then(profileRepository).should().findByUserId(senderId);
      then(notificationRepository).shouldHaveNoInteractions();
      then(eventPublisher).shouldHaveNoInteractions();
    }
  }

  @Nested
  @DisplayName("날씨 알림 테스트")
  class SendWeatherForecastNotificationTests {

    @Test
    @DisplayName("성공 - 날씨 알림을 정상적으로 전송한다")
    void sendWeatherForecastNotification_Success_ShouldSendNotification() {
      // given
      String title = "기온 변화 알림";
      String content = "기존 동일 예보 대비 7f℃ 변했습니다.";
      given(notificationRepository.save(any(Notification.class))).willReturn(notification);
      given(notificationMapper.toDto(any(Notification.class))).willReturn(notificationDto);

      // when
      sendNotificationService.sendWeatherForecastNotification(receiver, title, content);

      // then
      then(notificationRepository).should().save(any(Notification.class));
      then(eventPublisher).should().publishEvent(any(NotificationEvent.class));
    }
  }
}

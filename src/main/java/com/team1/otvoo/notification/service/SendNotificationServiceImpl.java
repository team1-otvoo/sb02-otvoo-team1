package com.team1.otvoo.notification.service;

import com.team1.otvoo.clothes.entity.ClothesAttributeDefinition;
import com.team1.otvoo.comment.entity.FeedComment;
import com.team1.otvoo.directmessage.entity.DirectMessage;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.feed.entity.Feed;
import com.team1.otvoo.feed.entity.FeedLike;
import com.team1.otvoo.follow.repository.FollowRepository;
import com.team1.otvoo.notification.dto.NotificationDto;
import com.team1.otvoo.notification.entity.Notification;
import com.team1.otvoo.notification.entity.NotificationLevel;
import com.team1.otvoo.notification.entity.NotificationType;
import com.team1.otvoo.notification.mapper.NotificationMapper;
import com.team1.otvoo.notification.repository.NotificationRepository;
import com.team1.otvoo.sse.event.RedisStreamService;
import com.team1.otvoo.sse.model.SseMessage;
import com.team1.otvoo.user.entity.Profile;
import com.team1.otvoo.user.entity.Role;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.ProfileRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SendNotificationServiceImpl implements SendNotificationService {

  private final NotificationRepository notificationRepository;
  private final ProfileRepository profileRepository;
  private final FollowRepository followRepository;
  private final RedisStreamService redisStreamService;
  private final NotificationMapper notificationMapper;

  @Override
  public void sendUserRoleNotification(Role previousUserRole, User user) {
    Role currentRole = user.getRole();

    String title = NotificationType.ROLE_CHANGE.formatTitle();
    String content = NotificationType.ROLE_CHANGE.formatContent(previousUserRole.name(), currentRole.name());
    createAndSendNotification(user, title, content);

    log.info("권한 변경 전송: {} -> {}", previousUserRole.name(), currentRole.name());
  }

  @Override
  public void sendClothesAttributeNotification(String methodType,
      ClothesAttributeDefinition clothesAttributeDefinition) {
    NotificationType type = methodType.equals("CREATE")
        ? NotificationType.ADD_ATTRIBUTION
        : NotificationType.UPDATE_ATTRIBUTION;

    String attributeName = clothesAttributeDefinition.getName();
    String title = type.getTitle();
    String content = type.formatContent(attributeName);
    createAndSendBroadcastNotification(title, content);

    log.info("의상 속성 변경(추가/수정) 알림 전송: {}", attributeName);
  }

  @Override
  public void sendLikeNotification(FeedLike feedLike) {
    User likedBy = feedLike.getLikedBy();
    UUID likedById = likedBy.getId();
    User receiver = feedLike.getFeed().getUser();

    Profile likedByProfile = findProfileByUserId(likedById);
    String title = NotificationType.LIKE_MY_FEED.formatTitle(likedByProfile.getName());
    String content = NotificationType.LIKE_MY_FEED.getContent();
    createAndSendNotification(receiver, title, content);

    log.info("좋아요 알림 전송: {} -> {}", likedById, receiver.getId());
  }

  @Override
  public void sendCommentNotification(FeedComment feedComment) {
    User author = feedComment.getUser();
    UUID authorId = author.getId();
    User receiver = feedComment.getFeed().getUser();

    Profile authorProfile = findProfileByUserId(authorId);
    String title = NotificationType.COMMENT_ON_MY_FEED.formatTitle(authorProfile.getName());
    String content = NotificationType.COMMENT_ON_MY_FEED.formatContent(feedComment.getContent());
    createAndSendNotification(receiver, title, content);

    log.info("댓글 작성 알림 전송: {} -> {}", authorId, receiver.getId());
  }

  @Override
  public void sendFeedNotification(Feed feed) {
    User followee = feed.getUser();
    Profile followeeProfile = findProfileByUserId(followee.getId());

    String title = NotificationType.FOLLOWEE_ADD_FEED.formatTitle(followeeProfile.getName());
    String content = NotificationType.FOLLOWEE_ADD_FEED.formatContent(feed.getContent());
    NotificationLevel level = NotificationLevel.INFO;

    Pageable pageable = PageRequest.of(0, 1000); // 1000명 단위로 처리
    Page<User> followerPage;
    List<User> followers;

    do {
      // 1. 팔로워를 배치 단위로 조회
      followerPage = followRepository.findFollowersByFolloweeId(feed.getUser().getId(), pageable);
      followers = followerPage.getContent();

      if (followers.isEmpty()) {
        return;
      }

      // 2. 각 팔로워에 대한 알림 엔티티 리스트 생성
      List<Notification> notifications = followers.stream()
          .map(follower -> new Notification(follower, title, content, level))
          .toList();
      log.info("배치 생성된 알림 수: {}", notifications.size());

      // 3. Bulk Insert를 통해 한 번의 쿼리로 모든 알림 저장 (N+1 해결)
      List<Notification> savedNotifications = notificationRepository.saveAll(notifications);
      log.info("배치 DB 저장 완료: {}개 저장", savedNotifications.size());

      // 4. SSE 메시지 전송
      // * 메시지 큐 or 배치 처리 도입 고려
      savedNotifications.forEach(notification -> {
        NotificationDto notificationDto = notificationMapper.toDto(notification);

        SseMessage message = SseMessage.builder()
            .eventId(UUID.randomUUID())
            .receiverIds(Set.of(notification.getReceiver().getId()))
            .broadcast(false)
            .eventName("notifications")
            .eventData(notificationDto)
            .createdAt(Instant.now())
            .build();

        redisStreamService.publish(message);
      });

      pageable = followerPage.nextPageable();
    } while(followerPage.hasNext());
    log.info("피드 등록 알림 전송: {} -> {}명의 팔로워", followee.getId(), followers.size());
  }

  @Override
  public void sendFollowNotification(User follower, User receiver) {
    Profile followerProfile = findProfileByUserId(follower.getId());
    String title = NotificationType.FOLLOW.formatTitle(followerProfile.getName());
    String content = NotificationType.FOLLOW.getContent();

    createAndSendNotification(receiver, title, content);
    log.info("팔로우 알림 전송: {} -> {}", follower.getId(), receiver.getId());
  }

  @Override
  public void sendDirectMessageNotification(DirectMessage directMessage) {
    User sender = directMessage.getSender();
    User receiver = directMessage.getReceiver();

    Profile senderProfile = findProfileByUserId(sender.getId());
    String title = NotificationType.RECEIVE_DM.formatTitle(senderProfile.getName());
    String content = NotificationType.RECEIVE_DM.formatContent(directMessage.getContent());
    createAndSendNotification(receiver, title, content);

    log.info("DM 수신 알림 전송: {} -> {}", sender.getId(), receiver.getId());
  }

  @Override
  public void sendWeatherForecastNotification(User receiver, String title, String content) {
    createAndSendNotification(receiver, title, content);

    log.info("날씨 알림 전송: {} -> {}", title, receiver.getId());
  }

  /*****************************
   * helper method
   *****************************/
  private void createAndSendNotification(User receiver, String title, String content) {
    Notification notification = new Notification(receiver, title, content, NotificationLevel.INFO);
    Notification saved = notificationRepository.save(notification);
    NotificationDto notificationDto = notificationMapper.toDto(saved);

    log.info("SseMessage로 변환 전 notificationDto {}", notificationDto);
    SseMessage message = SseMessage.builder()
        .eventId(UUID.randomUUID())
        .receiverIds(Set.of(receiver.getId()))
        .eventName("notifications")
        .eventData(notificationDto)
        .createdAt(Instant.now())
        .build();

    redisStreamService.publish(message);
  }

  private void createAndSendBroadcastNotification(String title, String content) {
    Notification notification = new Notification(null, title, content, NotificationLevel.INFO);
    Notification saved = notificationRepository.save(notification);
    NotificationDto notificationDto = notificationMapper.toDto(saved);

    log.info("SseMessage로 변환 전 notificationDto {}", notificationDto);
    SseMessage message = SseMessage.builder()
        .eventId(UUID.randomUUID())
        .broadcast(true)
        .eventName("notifications")
        .eventData(notificationDto)
        .createdAt(Instant.now())
        .build();

    redisStreamService.publish(message);
  }

  private Profile findProfileByUserId(UUID userId) {
    return profileRepository.findByUserId(userId)
        .orElseThrow(() -> new RestException(ErrorCode.NOT_FOUND, Map.of("id", userId)));
  }

}

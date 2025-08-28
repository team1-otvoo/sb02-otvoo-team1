package com.team1.otvoo.notification.service;

import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.notification.dto.NotificationDto;
import com.team1.otvoo.notification.dto.NotificationDtoCursorResponse;
import com.team1.otvoo.notification.entity.Notification;
import com.team1.otvoo.notification.entity.NotificationReadStatus;
import com.team1.otvoo.notification.mapper.NotificationMapper;
import com.team1.otvoo.notification.repository.NotificationReadStatusRepository;
import com.team1.otvoo.notification.repository.NotificationRepository;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.UserRepository;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;
  private final NotificationReadStatusRepository readStatusRepository;
  private final NotificationMapper notificationMapper;

  @Transactional(readOnly = true)
  @Override
  public NotificationDtoCursorResponse getList(UUID receiverId, String cursor, UUID idAfter,
      int limit) {

    Instant cursorInstant = null;
    if (cursor != null) {
      try {
        cursorInstant = Instant.parse(cursor);
      } catch (DateTimeParseException e) {
        throw new IllegalArgumentException("Invalid cursor format. Please use ISO-8601 format.", e);
      }
    }

    // hasNext 판별 위해 limit+1 개로 조회
    List<Notification> notifications = notificationRepository.findUnreadNotificationsWithCursor(
        receiverId, cursorInstant, idAfter, limit + 1);

    boolean hasNext = notifications.size() > limit;
    List<Notification> notificationList = hasNext ? notifications.subList(0, limit) : notifications;

    // 조회 결과가 없을 때
    if (notificationList.isEmpty()) {
      return new NotificationDtoCursorResponse(
          Collections.emptyList(),
          null,
          null,
          false,
          0L
      );
    }

    List<NotificationDto> data = notificationMapper.toDtoList(notificationList);
    long totalCount = notificationRepository.countUnreadNotifications(receiverId);

    Notification lastNotification = notificationList.get(notificationList.size() - 1);
    String nextCursor = lastNotification.getCreatedAt().toString();
    UUID nextIdAfter = lastNotification.getId();

    return new NotificationDtoCursorResponse(
        data,
        nextCursor,
        nextIdAfter,
        hasNext,
        totalCount
    );
  }

  @Transactional
  @Override
  public void readNotification(UUID notificationId, UUID userId) {
    Notification notification = notificationRepository.findById(notificationId)
        .orElseThrow(() -> new RestException(ErrorCode.NOT_FOUND, Map.of("id", notificationId)));

    User receiver = notification.getReceiver();
    // 개인 알림인 경우
    if (receiver != null) {
      // 본인의 알림이 맞는지 확인
      if (!receiver.getId().equals(userId)) {
        throw new RestException(ErrorCode.ACCESS_DENIED, Map.of("id", receiver.getId()));
      }
      notificationRepository.delete(notification);
    }
    // 브로드캐스트 알림인 경우
    else {
      // 이미 읽음 처리되었는지 확인
      boolean alreadyRead = readStatusRepository.existsByUserIdAndNotificationId(userId,
          notificationId);
      if (alreadyRead) {
        log.debug("이미 읽음 처리된 브로드캐스트 알림입니다. notificationId={}, userId={}", notificationId, userId);
        return;
      }

      User user = userRepository.findById(userId)
          .orElseThrow(() -> new RestException(ErrorCode.NOT_FOUND, Map.of("userId", userId)));

      NotificationReadStatus readStatus = new NotificationReadStatus(user, notification);
      readStatusRepository.save(readStatus);
    }
  }

  @Transactional
  @Scheduled(cron = "0 0 4 * * *")
  @Override
  public void cleanupOldNotifications() {
    log.info("오래된 브로드캐스트 알림 데이터 정리 시작");
    Instant cutoffDateTime = Instant.now().minus(7, ChronoUnit.DAYS);
    try {
      long deletedStatusCount = readStatusRepository.deleteByCreatedAtBefore(cutoffDateTime);
      log.info("Deleted {} notification read statuses older than 7 days", deletedStatusCount);

      long deletedNotificationCount = notificationRepository.deleteByReceiverIsNullAndCreatedAtBefore(
          cutoffDateTime);
      log.info("Deleted {} broadcast notifications older than 7 days", deletedNotificationCount);
    } catch (Exception e) {
      log.error("Error while deleting old notifications", e);
    }
  }

}

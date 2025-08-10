package com.team1.otvoo.notification.service;

import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.notification.dto.NotificationDto;
import com.team1.otvoo.notification.dto.NotificationDtoCursorResponse;
import com.team1.otvoo.notification.entity.Notification;
import com.team1.otvoo.notification.mapper.NotificationMapper;
import com.team1.otvoo.notification.repository.NotificationRepository;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

  private final NotificationRepository notificationRepository;
  private final NotificationMapper notificationMapper;

  @Transactional(readOnly = true)
  @Override
  public NotificationDtoCursorResponse getList(UUID receiverId, String cursor, UUID idAfter, int limit) {

    Instant cursorInstant = null;
    if (cursor != null) {
      try {
        cursorInstant = Instant.parse(cursor);
      } catch (DateTimeParseException e) {
        throw new IllegalArgumentException("Invalid cursor format. Please use ISO-8601 format.", e);
      }
    }

    // hasNext 판별 위해 limit+1 개로 조회
    List<Notification> notifications = notificationRepository.findNotificationsWithCursor(receiverId, cursorInstant, idAfter, limit+1);

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
    long totalCount = notificationRepository.countByReceiverId(receiverId);

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
  public void delete(UUID notificationId) {
    Notification notification = notificationRepository.findById(notificationId)
        .orElseThrow(() -> new RestException(ErrorCode.NOT_FOUND, Map.of("id", notificationId)));

    notificationRepository.delete(notification);
  }
}

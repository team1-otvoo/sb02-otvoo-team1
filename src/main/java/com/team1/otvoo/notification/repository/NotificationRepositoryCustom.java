package com.team1.otvoo.notification.repository;

import com.team1.otvoo.notification.entity.Notification;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationRepositoryCustom {
  List<Notification> findNotificationsWithCursor(UUID receiverId, Instant cursor, UUID idAfter, int limit);

}

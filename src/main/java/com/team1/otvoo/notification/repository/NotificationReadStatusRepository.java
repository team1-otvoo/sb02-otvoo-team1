package com.team1.otvoo.notification.repository;

import com.team1.otvoo.notification.entity.NotificationReadStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationReadStatusRepository extends JpaRepository<NotificationReadStatus, UUID> {
  boolean existsByUserIdAndNotificationId(UUID userId, UUID notificationId);
  long deleteByCreatedAtBefore(Instant cutoffDateTime);
}

package com.team1.otvoo.notification.repository;

import com.team1.otvoo.notification.entity.Notification;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID>, NotificationRepositoryCustom {
  long deleteByReceiverIsNullAndCreatedAtBefore(Instant cutoffDateTime);
}

package com.team1.otvoo.notification.repository;

import com.team1.otvoo.notification.entity.Notification;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID>, NotificationRepositoryCustom {
  @Query("SELECT COUNT(n) FROM Notification n WHERE n.receiver.id = :receiverId")
  long countByReceiverId(@Param("receiverId") UUID receiverId);
}

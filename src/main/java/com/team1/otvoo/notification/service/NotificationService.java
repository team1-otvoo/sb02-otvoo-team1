package com.team1.otvoo.notification.service;

import com.team1.otvoo.notification.dto.NotificationDtoCursorResponse;
import com.team1.otvoo.user.entity.User;
import java.util.UUID;

public interface NotificationService {
  NotificationDtoCursorResponse getList(UUID receiverId, String cursor, UUID idAfter, int limit);
  void delete(UUID notificationId);
}

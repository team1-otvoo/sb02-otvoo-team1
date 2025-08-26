package com.team1.otvoo.notification.event;

import com.team1.otvoo.notification.dto.NotificationDto;
import java.util.List;

public record NotificationEvent(
    List<NotificationDto> notificationDtoList,
    boolean broadcast
) {
  public NotificationEvent(NotificationDto dto, boolean broadcast) {
    this(List.of(dto), broadcast);
  }
}

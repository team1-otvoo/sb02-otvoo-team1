package com.team1.otvoo.notification.mapper;

import com.team1.otvoo.notification.dto.NotificationDto;
import com.team1.otvoo.notification.entity.Notification;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
  @Mapping(source = "receiver.id", target = "receiverId")
  NotificationDto toDto(Notification Notification);
  List<NotificationDto> toDtoList(List<Notification> Notification);
}

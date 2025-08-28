package com.team1.otvoo.notification.entity;

import com.team1.otvoo.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_read_status", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "notification_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationReadStatus {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "notification_id", nullable = false)
  private Notification notification;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public NotificationReadStatus(User user, Notification notification) {
    this.user = user;
    this.notification = notification;
    this.createdAt = Instant.now();
  }

}

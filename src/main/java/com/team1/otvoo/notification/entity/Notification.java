package com.team1.otvoo.notification.entity;

import com.team1.otvoo.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "receiver_id", nullable = true)
  private User receiver;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private NotificationLevel level;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public Notification (User receiver, String title, String content, NotificationLevel level) {
    this.receiver = receiver;
    this.title = title;
    this.content = content;
    this.level = level;
    this.createdAt = Instant.now();
  }

}

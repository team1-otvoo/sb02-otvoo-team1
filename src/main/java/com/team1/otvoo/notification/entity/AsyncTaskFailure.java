package com.team1.otvoo.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "async_task_failures")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AsyncTaskFailure {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;


  @Column(name = "event_id", nullable = false)
  private UUID eventId;


  @Column(columnDefinition = "text")
  private String payload;


  @Column(columnDefinition = "text")
  private String error;


  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Builder
  public AsyncTaskFailure (UUID eventId, String payload, String error) {
    this.eventId = eventId;
    this.payload = payload;
    this.error = error;
    this.createdAt = Instant.now();
  }

}

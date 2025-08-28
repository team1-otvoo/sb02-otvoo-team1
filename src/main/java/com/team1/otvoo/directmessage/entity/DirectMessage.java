package com.team1.otvoo.directmessage.entity;

import com.team1.otvoo.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "direct_messages")
public class DirectMessage {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_id", nullable = false)
  private User sender;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "receiver_id", nullable = false)
  private User receiver;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String content;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
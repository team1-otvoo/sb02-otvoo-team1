package com.team1.otvoo.follow.entity;

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
@Table(name = "follows", uniqueConstraints = {
    @UniqueConstraint(
        name = "uq_followee_follower",
        columnNames = {"followee_id", "follower_id"}
    )
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Follow {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "followee_id", nullable = false)
  private User followee;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "follower_id", nullable = false)
  private User follower;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public Follow (User followee, User follower) {
    this.followee = followee;
    this.follower = follower;
    this.createdAt = Instant.now();
  }

}
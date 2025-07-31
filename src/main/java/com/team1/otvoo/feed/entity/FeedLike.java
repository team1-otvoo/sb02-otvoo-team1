package com.team1.otvoo.feed.entity;

import com.team1.otvoo.comment.entity.FeedComment;
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
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "feed_likes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedLike {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "comment_id", nullable = false)
  private FeedComment comment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "liked_by")
  private User likedBy;

  @Column(name = "created_at")
  private Instant createdAt;

  public FeedLike(FeedComment feedComment, User user) {
    this.comment = feedComment;
    this.likedBy = user;
    this.createdAt = Instant.now();
  }
}

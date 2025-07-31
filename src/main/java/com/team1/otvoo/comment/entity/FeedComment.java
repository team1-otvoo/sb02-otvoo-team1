package com.team1.otvoo.comment.entity;

import com.team1.otvoo.feed.entity.Feed;
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
@Table(name = "feed_comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedComment {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "feed_id", nullable = false)
  private Feed feed;

  @Column(name = "content", nullable = false)
  private String content;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "is_deleted")
  private boolean isDeleted;

  public FeedComment(User user, Feed feed, String content) {
    this.user = user;
    this.feed = feed;
    this.content = content;
    this.createdAt = Instant.now();
    this.isDeleted = false;
  }
}

package com.team1.otvoo.feed.entity;

import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.weather.entity.WeatherForecast;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "feeds")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feed {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @OneToMany(mappedBy = "feed", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<FeedRecommendation> feedRecommendations;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "forecast_id")
  private WeatherForecast forecast;

  @Column(name = "content")
  private String content;

  @Column(name = "like_count")
  private long likeCount;

  @Column(name = "comment_count")
  private long commentCount;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @Column(name = "is_deleted")
  private boolean isDeleted;

  public Feed(User user, List<FeedRecommendation> feedRecommendationList, WeatherForecast weatherForecast, String content) {
    this.user = user;
    this.feedRecommendations = feedRecommendationList;
    this.forecast = weatherForecast;
    this.content = content;
    this.likeCount = 0L;
    this.createdAt = Instant.now();
    this.isDeleted = false;
  }
}
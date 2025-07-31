package com.team1.otvoo.recommendation.entity;

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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recommendations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recommendation {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "weather_id", nullable = false)
  private WeatherForecast weather;

  @Column(nullable = false)
  private Instant createdAt;

  @OneToMany(mappedBy = "recommendation", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<RecommendationClothes> clothes = new ArrayList<>();

  public Recommendation(User user, WeatherForecast weather) {
    this.user = user;
    this.weather = weather;
    this.createdAt = Instant.now();
  }

  public void addClothes(RecommendationClothes rc) {
    clothes.add(rc);
    rc.setRecommendation(this);
  }
}

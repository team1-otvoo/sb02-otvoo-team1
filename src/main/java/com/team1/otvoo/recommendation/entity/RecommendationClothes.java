package com.team1.otvoo.recommendation.entity;

import com.team1.otvoo.clothes.entity.Clothes;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recommendation_clothes")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationClothes {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recommendation_id", nullable = false)
  private Recommendation recommendation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "clothes_id", nullable = false)
  private Clothes clothes;

  public RecommendationClothes(Clothes clothes) {
    this.clothes = clothes;
  }

  public void setRecommendation(Recommendation recommendation) {
    this.recommendation = recommendation;
  }

}

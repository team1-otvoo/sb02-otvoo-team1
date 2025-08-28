package com.team1.otvoo.recommendation.repository;

import com.team1.otvoo.recommendation.entity.Recommendation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {
  Optional<Recommendation> findByWeather_IdAndUser_Id(UUID weatherId, UUID userId);
  void deleteAllByWeather_Id(UUID weatherId);
}

package com.team1.otvoo.recommendation.repository;

import com.team1.otvoo.recommendation.entity.Recommendation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
  
  public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {

  }

package com.team1.otvoo.recommendation.service;

import com.team1.otvoo.recommendation.dto.RecommendationDto;
import java.util.UUID;

public interface RecommendationService {
  RecommendationDto get(UUID weatherId);
}

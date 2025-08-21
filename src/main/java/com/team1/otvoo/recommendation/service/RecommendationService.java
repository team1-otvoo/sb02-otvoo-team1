package com.team1.otvoo.recommendation.service;

import com.team1.otvoo.recommendation.dto.FilteredClothesResponse;
import com.team1.otvoo.recommendation.dto.RecommendationDto;
import java.util.UUID;

public interface RecommendationService {
  RecommendationDto refresh(UUID weatherId);
  RecommendationDto get(UUID weatherId);
}

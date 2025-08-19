package com.team1.otvoo.recommendation.service;

import com.team1.otvoo.recommendation.dto.RecommendationDto;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RecommendationServiceImpl implements RecommendationService{

  @Override
  public RecommendationDto get(UUID weatherId) {
    return null;
  }
}

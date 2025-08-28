package com.team1.otvoo.recommendation.controller;

import com.team1.otvoo.recommendation.client.OpenAiClient;
import com.team1.otvoo.recommendation.dto.VisionAttributeResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
public class AiTestController {

  private final OpenAiClient client;

  @GetMapping("/test")
  public VisionAttributeResponseDto analyzeImage(@RequestParam String imageUrl) {
    return client.analyzeImage(imageUrl);
  }
}

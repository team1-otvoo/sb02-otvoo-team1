package com.team1.otvoo.clothes.event;

import com.team1.otvoo.recommendation.service.ClothesAiAttributeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClothesEventHandler {

  private final ClothesAiAttributeService clothesAiAttributeService;

  @TransactionalEventListener
  @Async
  public void handleClothesCreatedEvent(ClothesCreatedEvent event) {
    clothesAiAttributeService.extractAndSaveAttributes(event.clothes(), event.imageUrl());
  }
}

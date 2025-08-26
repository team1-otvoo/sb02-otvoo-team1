package com.team1.otvoo.clothes.event;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import com.team1.otvoo.clothes.dto.OotdDto;
import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeWithDefDto;
import com.team1.otvoo.clothes.entity.Clothes;
import com.team1.otvoo.clothes.entity.ClothesAttributeValue;
import com.team1.otvoo.clothes.repository.ClothesImageRepository;
import com.team1.otvoo.clothes.repository.ClothesRepository;
import com.team1.otvoo.feed.dto.FeedDto;
import com.team1.otvoo.feed.elasticsearch.document.FeedDocument;
import com.team1.otvoo.feed.elasticsearch.repository.FeedSearchRepository;
import com.team1.otvoo.feed.entity.FeedClothes;
import com.team1.otvoo.feed.mapper.FeedDocumentMapper;
import com.team1.otvoo.feed.repository.FeedClothesRepository;
import com.team1.otvoo.feed.repository.FeedRepositoryCustomImpl;
import com.team1.otvoo.notification.service.SendNotificationService;
import com.team1.otvoo.storage.S3ImageStorage;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClothesAttributeEventHandler {

  private final SendNotificationService SendNotificationService;
  private final ClothesRepository clothesRepository;
  private final ClothesImageRepository clothesImageRepository;
  private final FeedClothesRepository feedClothesRepository;
  private final FeedRepositoryCustomImpl feedRepositoryCustomImpl;
  private final FeedDocumentMapper feedDocumentMapper;
  private final FeedSearchRepository feedSearchRepository;
  private final S3ImageStorage s3ImageStorage;

  @Async
  @TransactionalEventListener
  public void handleEvent(ClothesAttributeEvent event) {
    try {
      SendNotificationService.sendClothesAttributeNotification(event.methodType(),
          event.savedClothesAttributeDefinition());
    } catch (Exception e) {
      log.error("의상 속성 알림 전송 실패: {}", e.getMessage(), e);
    }
  }

  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener
  public void handleClothesAttributeUpdatedEvent(ClothesAttributeUpdatedEvent event) {
    try {
      UUID definitionId = event.definitionId();

      // 1. 해당 속성 정의 ID를 참조하는 옷들 ID만 조회
      List<UUID> clothesIds = clothesRepository.findClothesIdsByDefinitionId(definitionId);
      if (clothesIds.isEmpty()) {
        log.info("의상 속성 변경 -> 관련 의상 없음, definitionId={}", definitionId);
        return;
      }

      // 2. 해당 옷들 전체 + selectedValues 전부 fetch join으로 로딩
      clothesRepository.findAllWithSelectedValuesByClothesIds(clothesIds);

      // 3. 의상 ID마다 관련된 FeedDto 조회
      List<FeedDto> feedDtos = clothesIds.stream()
          .flatMap(clothesId ->
              feedRepositoryCustomImpl.projectionFeedDtoByClothesId(clothesId).stream())
          .distinct()
          .toList();

      if (feedDtos.isEmpty()) {
        log.info("의상 속성 변경 -> 관련 피드 없음, definitionId={}", definitionId);
        return;
      }

      // 4. ootds 채우기 (feedIds 기준으로 feedClothes + Clothes + 속성 전부 로딩)
      Map<UUID, List<OotdDto>> ootdMap = feedDtos.stream()
          .collect(Collectors.toMap(
              FeedDto::getId,
              dto -> feedClothesRepository.findAllByClothesId(clothesIds)
                  .stream()
                  .filter(fc -> fc.getFeed().getId().equals(dto.getId()))
                  .map(fc -> {
                    Clothes c = fc.getClothes();
                    return OotdDto.builder()
                        .clothesId(c.getId())
                        .name(c.getName())
                        .type(c.getType())
                        .imageUrl(null)
                        .attributes(c.getSelectedValues().stream()
                            .map(sv -> new ClothesAttributeWithDefDto(
                                sv.getDefinition().getId(),
                                sv.getDefinition().getName(),
                                sv.getDefinition().getValues().stream()
                                    .map(ClothesAttributeValue::getValue)
                                    .toList(),
                                sv.getValue().getValue()
                            ))
                            .toList()
                        )
                        .build();
                  })
                  .toList()
          ));

      // 5. FeedDto에 ootds 주입
      feedDtos.forEach(dto ->
          dto.setOotds(ootdMap.getOrDefault(dto.getId(), List.of()))
      );

      // 6. FeedDocument 변환
      List<FeedDocument> documents = feedDtos.stream()
          .map(feedDocumentMapper::toDocument)
          .toList();

      // 7. Elasticsearch 저장
      feedSearchRepository.saveAll(documents);

      log.info("의상 속성 변경 -> ES 동기화 완료, feeds={}", feedDtos.size());

    } catch (DataAccessException | ElasticsearchException e) {
      log.error("의상 속성 변경 이벤트 처리 실패: {}", e.getMessage(), e);
    }
  }

  @Async
  @TransactionalEventListener
  public void handleClothesDeletedEvent(ClothesDeletedEvent event) {
    UUID clothesId = event.clothesId();
    try {
      // 1. projection으로 FeedDto 조회
      List<FeedDto> feedDtos = feedRepositoryCustomImpl.projectionFeedDtoByClothesId(clothesId);
      if (feedDtos.isEmpty()) {
        log.info("의상 삭제 -> 관련 피드 없음, clothesId={}", clothesId);
        return;
      }

      List<UUID> feedIds = feedDtos.stream().map(FeedDto::getId).toList();

      // 2. FeedClothes + Clothes + Attributes 조회
      List<FeedClothes> feedClothes = feedClothesRepository
          .findAllByFeedIdInWithClothesAndSelectedValues(feedIds);

      Map<UUID, List<OotdDto>> ootdMap = feedClothes.stream()
          .filter(fc -> !fc.getClothes().getId().equals(clothesId)) // 삭제된 옷 빼고 나머지만 남김
          .collect(Collectors.groupingBy(
              fc -> fc.getFeed().getId(),
              Collectors.mapping(fc -> {
                Clothes c = fc.getClothes();
                return OotdDto.builder()
                    .clothesId(c.getId())
                    .name(c.getName())
                    .type(c.getType())
                    .imageUrl(null) // presigned URL은 ES에 저장하지 않음
                    .attributes(c.getSelectedValues().stream()
                        .map(sv -> new ClothesAttributeWithDefDto(
                            sv.getDefinition().getId(),
                            sv.getDefinition().getName(),
                            sv.getDefinition().getValues().stream()
                                .map(ClothesAttributeValue::getValue)
                                .toList(),
                            sv.getValue().getValue()
                        ))
                        .toList()
                    )
                    .build();
              }, Collectors.toList())
          ));

      // 3. FeedDto에 ootds 덮어씌우기
      feedDtos.forEach(dto ->
          dto.setOotds(ootdMap.getOrDefault(dto.getId(), List.of()))
      );

      // 4. FeedDocument 변환 후 ES 저장
      List<FeedDocument> documents = feedDtos.stream()
          .map(feedDocumentMapper::toDocument)
          .toList();

      feedSearchRepository.saveAll(documents);

      log.info("의상 삭제 -> ES 동기화 완료, clothesId={}, feeds={}", clothesId, feedDtos.size());

    } catch (DataAccessException | ElasticsearchException e) {
      log.error("의상 삭제 이벤트 처리 실패: clothesId={}, error={}", clothesId, e.getMessage(), e);
    }
  }
}

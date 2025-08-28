package com.team1.otvoo.clothes.event;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import com.team1.otvoo.clothes.entity.Clothes;
import com.team1.otvoo.clothes.entity.ClothesImage;
import com.team1.otvoo.clothes.mapper.ClothesMapper;
import com.team1.otvoo.clothes.repository.ClothesImageRepository;
import com.team1.otvoo.clothes.repository.ClothesRepository;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.feed.elasticsearch.document.FeedDocument;
import com.team1.otvoo.feed.entity.Feed;
import com.team1.otvoo.feed.entity.FeedClothes;
import com.team1.otvoo.feed.repository.FeedClothesRepository;
import com.team1.otvoo.recommendation.dto.ElasticOotdDto;
import com.team1.otvoo.recommendation.service.ClothesAiAttributeService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClothesEventHandler {

  private final ClothesAiAttributeService clothesAiAttributeService;
  private final FeedClothesRepository feedClothesRepository;
  private final ClothesRepository clothesRepository;
  private final ClothesImageRepository clothesImageRepository;
  private final ElasticsearchOperations elasticsearchOperations;
  private final ClothesMapper clothesMapper;

  @TransactionalEventListener
  @Async
  public void handleClothesCreatedEvent(ClothesCreatedEvent event) {
    clothesAiAttributeService.extractAndSaveAttributes(event.clothes(), event.imageUrl());
  }

  @Async
  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener
  public void handleClothesUpdatedEvent_Partial_Bulk(ClothesUpdatedEvent event) {
    try {
      UUID clothesId = event.clothesId();

      // 1) 이 옷을 참조하는 feedIds
      List<UUID> feedIds = feedClothesRepository.findByClothes_Id(clothesId).stream()
          .map(FeedClothes::getFeed).map(Feed::getId).distinct().toList();
      if (feedIds.isEmpty()) {
        log.info("ES 부분 업데이트 스킵 - 참조 Feed 없음, clothesId={}", clothesId);
        return;
      }

      // 2) 변경된 OOTD 스냅샷 (DB 조회)
      Clothes clothes = clothesRepository.findById(clothesId)
          .orElseThrow(
              () -> new RestException(ErrorCode.CLOTHES_NOT_FOUND, Map.of("clothesId", clothesId)));
      var imgOpt = clothesImageRepository.findByClothes_Id(clothesId);

      ElasticOotdDto updatedOotd = new ElasticOotdDto(
          clothes.getId(),
          clothes.getName(),
          imgOpt.map(ClothesImage::getImageKey).orElse(null),
          imgOpt.map(ClothesImage::getContentType).orElse(null),
          clothes.getType(),
          clothes.getSelectedValues().stream().map(clothesMapper::toAttributeDefDto).toList()
      );

      var index = IndexCoordinates.of("feed_index");

      // 3) feedIds를 500개씩 배치로 나눠서 처리
      final int BATCH_SIZE = 500;
      for (int start = 0; start < feedIds.size(); start += BATCH_SIZE) {
        int end = Math.min(start + BATCH_SIZE, feedIds.size());
        List<String> batchIds = feedIds.subList(start, end).stream()
            .map(UUID::toString).toList();

        // 3-1) ids query로 한 번에 관련 문서 조회
        var query = new NativeQueryBuilder()
            .withQuery(q -> q.ids(i -> i.values(batchIds)))
            .withPageable(Pageable.unpaged())
            .build();

        SearchHits<FeedDocument> hits = elasticsearchOperations.search(query, FeedDocument.class,
            index);

        // id -> 문서 매핑 (SearchHit의 getId()가 ES _id)
        Map<String, FeedDocument> docMap = hits.getSearchHits().stream()
            .collect(Collectors.toMap(SearchHit::getId, SearchHit::getContent));

        // 3-2) 메모리에서 ootds만 교체 → 부분 업데이트 문서 생성
        List<UpdateQuery> queries = new ArrayList<>(batchIds.size());
        for (String id : batchIds) {
          FeedDocument current = docMap.get(id);
          if (current == null) {
            log.warn("ES 문서 없음 - feedId={}, clothesId={}", id, clothesId);
            continue;
          }

          List<ElasticOotdDto> ootds = new ArrayList<>(
              current.getOotds() == null ? List.of() : current.getOotds()
          );

          int idx = -1;
          for (int i = 0; i < ootds.size(); i++) {
            var o = ootds.get(i);
            if (o.getClothesId() != null && o.getClothesId().equals(clothesId)) {
              idx = i;
              break;
            }
          }
          if (idx >= 0) {
            ootds.set(idx, updatedOotd);
          } else {
            ootds.add(updatedOotd); // 필요 시 추가
          }

          var partial = Document.create();
          partial.put("ootds", ootds);
          partial.put("updatedAt", Instant.now().toEpochMilli());

          queries.add(UpdateQuery.builder(id)
              .withDocument(partial)
              .withDocAsUpsert(false)
              .build());
        }

        if (!queries.isEmpty()) {
          elasticsearchOperations.bulkUpdate(queries, index);
          log.info("ES 부분 업데이트 BULK 완료(batch) - clothesId={}, updatedDocs={}", clothesId,
              queries.size());
        }
      }
    } catch (DataAccessException | ElasticsearchException e) {
      log.error("ES 부분 업데이트 BULK 실패", e);
    }
  }
}

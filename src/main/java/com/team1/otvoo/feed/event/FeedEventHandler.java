package com.team1.otvoo.feed.event;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import com.team1.otvoo.feed.dto.FeedDto;
import com.team1.otvoo.feed.elasticsearch.document.FeedDocument;
import com.team1.otvoo.feed.elasticsearch.repository.FeedSearchRepository;
import com.team1.otvoo.feed.mapper.FeedDocumentMapper;
import com.team1.otvoo.notification.service.SendNotificationService;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.ScriptType;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.core.query.UpdateResponse;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeedEventHandler {

  private final ElasticsearchOperations elasticsearchOperations;
  private final SendNotificationService SendNotificationService;
  private final FeedSearchRepository feedSearchRepository;
  private final FeedDocumentMapper feedDocumentMapper;

  @Async
  @TransactionalEventListener
  public void handleEvent(FeedEvent event) {
    try {
      SendNotificationService.sendFeedNotification(event.savedFeed());
    } catch (Exception e) {
      log.error("피드 알림 전송 실패: {}", e.getMessage(), e);
    }
  }

  @Async
  @TransactionalEventListener
  public void handleLikeEvent(FeedLikeEvent event) {
    try {
      SendNotificationService.sendLikeNotification(event.savedFeedLike());
    } catch (Exception e) {
      log.error("좋아요 알림 전송 실패: {}", e.getMessage(), e);
    }
  }

  @Async
  @TransactionalEventListener
  public void handleLikeCreatedEvent(FeedLikeEvent event) {
    UUID feedId = event.savedFeedLike().getFeed().getId();
    try {
      Map<String, Object> params = Map.of("inc", 1);

      UpdateQuery updateQuery = UpdateQuery.builder(feedId.toString())
          .withScript("ctx._source.likeCount += params.inc")
          .withParams(params)
          .withScriptType(ScriptType.INLINE)
          .build();

      UpdateResponse response = elasticsearchOperations.update(
          updateQuery,
          IndexCoordinates.of("feed_index")
      );

      log.info("Elasticsearch likeCount 증가 성공 - feedId={}, result={}", feedId,
          response.getResult());

    } catch (DataAccessException | ElasticsearchException e) {
      log.error("elasticsearch likeCount 증가 실패 - feedId: {}", feedId, e);
    }
  }

  @Async
  @TransactionalEventListener
  public void handleUnLikeCreatedEvent(FeedUnlikeEvent event) {
    UUID feedId = event.feedId();
    try {
      Map<String, Object> params = Map.of("inc", 1);

      UpdateQuery updateQuery = UpdateQuery.builder(feedId.toString())
          .withScript("ctx._source.likeCount -= params.inc")
          .withParams(params)
          .withScriptType(ScriptType.INLINE)
          .build();

      UpdateResponse response = elasticsearchOperations.update(
          updateQuery,
          IndexCoordinates.of("feed_index")
      );

      log.info("Elasticsearch likeCount 감소 성공 - feedId={}, result={}", feedId,
          response.getResult());

    } catch (DataAccessException | ElasticsearchException e) {
      log.error("Elasticsearch likeCount 감소 실패 - feedId: {}", feedId, e);
    }
  }

  @Async
  @TransactionalEventListener
  public void handleFeedDeletedEvent(FeedDeletedEvent event) {
    try {
      feedSearchRepository.deleteById(event.feedId());
      log.info("Elasticsearch 문서 삭제 완료: {}", event.feedId());
    } catch (DataAccessException | ElasticsearchException e) {
      log.error("Elasticsearch 문서 삭제 실패 - feedId: {}", event.feedId(), e);
    }
  }

  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener
  public void handleFeedCreatedEvent(FeedCreatedEvent event) {
    try {
      FeedDto feedDto = event.feedDto();
      FeedDocument feedDocument = feedDocumentMapper.toDocument(feedDto);
      feedSearchRepository.save(feedDocument);
      log.info("Elasticsearch 색인 완료: {}", feedDto.getId());
    } catch (DataAccessException | ElasticsearchException e) {
      log.error("Elasticsearch 색인 실패 - feedId: {}", event.feedDto().getId(), e);
    }
  }

  @Async
  @TransactionalEventListener
  public void handleFeedUpdatedEvent(FeedUpdatedEvent event) {
    try {
      FeedDto feedDto = event.feedDto();
      FeedDocument feedDocument = feedDocumentMapper.toDocument(feedDto);
      feedSearchRepository.save(feedDocument);
      log.info("Elasticsearch 색인 업데이트 완료: {}", feedDto.getId());
    } catch (DataAccessException | ElasticsearchException e) {
      log.error("Elasticsearch 색인 업데이트 실패 - feedId: {}", event.feedDto().getId(), e);
    }
  }

}

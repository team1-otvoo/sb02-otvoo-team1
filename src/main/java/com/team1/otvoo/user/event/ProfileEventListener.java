package com.team1.otvoo.user.event;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import com.team1.otvoo.user.repository.ProfileRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.ScriptType;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileEventListener {

  private final ElasticsearchOperations elasticsearchOperations;
  private final ProfileRepository profileRepository;

  @TransactionalEventListener
  @Async
  public void handleProfileUpdatedEvent(ProfileUpdatedEvent event) {
    final IndexCoordinates index = IndexCoordinates.of("feed_index");
    UUID userId = event.userId();
    String newName = event.name();

    if(newName == null) {
      newName = profileRepository.findByUserId(userId).get().getName();
    }

    try {
      // 1) author.userId 가 해당 userId 인 문서만
      var query = new NativeQueryBuilder()
          .withQuery(q -> q.term(t -> t.field("author.userId").value(userId.toString())))
          .build();

      // 2) author.name 만 교체
      Map<String, Object> params = Map.of("name", newName);
      UpdateQuery updateQuery = UpdateQuery.builder(query)
          .withScript("ctx._source.author.name = params.name")
          .withParams(params)
          .withScriptType(ScriptType.INLINE)
          .build();

      // 3) update
      var response = elasticsearchOperations.updateByQuery(updateQuery, index);

      log.info("Elasticsearch author.name 수정 성공 - userId={}, response={}", userId, response);
    } catch (DataAccessException | ElasticsearchException e) {
      log.error("Elasticsearch author.name 수정 실패 - userId={}", userId, e);
    }
  }
}

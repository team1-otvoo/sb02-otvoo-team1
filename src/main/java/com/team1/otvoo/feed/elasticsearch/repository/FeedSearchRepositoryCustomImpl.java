package com.team1.otvoo.feed.elasticsearch.repository;

import static co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import com.team1.otvoo.clothes.dto.OotdDto;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.feed.dto.FeedDto;
import com.team1.otvoo.feed.dto.FeedSearchCondition;
import com.team1.otvoo.feed.elasticsearch.document.FeedDocument;
import com.team1.otvoo.storage.S3ImageStorage;
import com.team1.otvoo.user.repository.ProfileRepository;
import com.team1.otvoo.user.resolver.ProfileImageUrlResolver;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;


@RequiredArgsConstructor
public class FeedSearchRepositoryCustomImpl implements FeedSearchRepositoryCustom {
  private final ElasticsearchOperations elasticsearchOperations;
  private final S3ImageStorage s3ImageStorage;
  private final ProfileImageUrlResolver profileImageUrlResolver;
  private final ProfileRepository profileRepository;

  @Override
  public Slice<FeedDto> searchFeeds(FeedSearchCondition searchCondition) {
    Builder boolQuery = QueryBuilders.bool();

    // keyword 검색
    if (searchCondition.keywordLike() != null && !searchCondition.keywordLike().isBlank()) {
      boolQuery.must(m -> m.multiMatch(mm -> mm
          .query(searchCondition.keywordLike())
          .fields(List.of("content", "content.ngram"))
      ));
    }

    // SkyStatus 필터
    if (searchCondition.skyStatusEqual() != null) {
      boolQuery.filter(
          f -> f.term(t -> t.field("weather.skyStatus").value(searchCondition.skyStatusEqual().name())));
    }

    // PrecipitationType 필터
    if (searchCondition.precipitationTypeEqual() != null) {
      boolQuery.filter(f -> f.term(t -> t.field("weather.precipitation.type")
          .value(searchCondition.precipitationTypeEqual().name())));
    }

    // 정렬
    String sortBy = (searchCondition.sortBy() != null) ? searchCondition.sortBy() : "createdAt";
    SortOrder order = "asc".equalsIgnoreCase(searchCondition.sortDirection())
        ? SortOrder.Asc
        : SortOrder.Desc;

    List<SortOptions> sorts = new ArrayList<>();

    if ("likeCount".equalsIgnoreCase(sortBy)) {
      // likeCount 정렬 → tie-breaker: created_at, feed_id
      sorts.add(SortOptions.of(s -> s.field(f -> f.field("likeCount").order(order))));
      sorts.add(SortOptions.of(s -> s.field(f -> f.field("createdAt").order(SortOrder.Desc)))); // tie-breaker 1
      sorts.add(SortOptions.of(s -> s.field(f -> f.field("feedId").order(SortOrder.Asc)))); //  tie-breaker 2
    } else {
      // createdAt 정렬 → tie-breaker: feed_id
      sorts.add(SortOptions.of(s -> s.field(f -> f.field("createdAt").order(order))));
      sorts.add(SortOptions.of(s -> s.field(f -> f.field("feedId").order(SortOrder.Asc)))); // tie-breaker 1
    }

    // search_after (커서)
    List<Object> searchAfter = null;
    if (searchCondition.cursor() != null) {
      if ("likeCount".equalsIgnoreCase(sortBy)) {
        // cursor 형태: "{likeCount}_{createdAt}"
        String[] cursorList = searchCondition.cursor().split("_");
        long countCursorValue = Long.parseLong(cursorList[0]);
        long createdAtCursorValue = Instant.parse(cursorList[1]).toEpochMilli();
        UUID idAfter = searchCondition.idAfter();

        searchAfter = List.of(countCursorValue, createdAtCursorValue, idAfter.toString());
      } else {
        // createdAt 기준 정렬
        long createdAtCursorValue = Instant.parse(searchCondition.cursor()).toEpochMilli();
        UUID idAfter = searchCondition.idAfter();

        searchAfter = List.of(createdAtCursorValue, idAfter.toString());
      }
    }

    // NativeQuery 생성
    NativeQueryBuilder queryBuilder = new NativeQueryBuilder()
        .withQuery(boolQuery.build()._toQuery())
        .withSort(sorts)
        .withPageable(PageRequest.of(0, searchCondition.limit()));

    if (searchAfter != null) {
      queryBuilder.withSearchAfter(searchAfter);
    }

    NativeQuery query = queryBuilder.build();

    // 실행
    SearchHits<FeedDocument> hits = elasticsearchOperations.search(query, FeedDocument.class);

    List<FeedDto> content = hits.getSearchHits().stream()
        .map(hit -> toDto(hit.getContent()))
        .toList();

    boolean hasNext = content.size() == searchCondition.limit();

    return new SliceImpl<>(content, PageRequest.of(0, searchCondition.limit()), hasNext);
  }

  private FeedDto toDto(FeedDocument doc) {
    FeedDto feedDto = new FeedDto(
        UUID.fromString(doc.getFeedId()),
        doc.getCreatedAt() != null ? Instant.ofEpochMilli(doc.getCreatedAt()) : null,
        doc.getUpdatedAt() != null ? Instant.ofEpochMilli(doc.getUpdatedAt()) : null,
        doc.getAuthor(),
        doc.getWeather(),
        doc.getOotds().stream().map(
            eo -> OotdDto.builder()
                .type(eo.getType())
                .imageUrl(s3ImageStorage.getPresignedUrl(eo.getImageKey(), eo.getContentType()))
                .clothesId(eo.getClothesId())
                .attributes(eo.getAttributes())
                .name(eo.getName())
                .build()
        ).toList(),
        doc.getContent(),
        doc.getLikeCount(),
        0,
        false
    );
    UUID profileId = profileRepository.findByUserId(feedDto.getAuthor().getUserId()).orElseThrow(
        () -> new RestException(ErrorCode.PROFILE_NOT_FOUND, Map.of("userId", feedDto.getAuthor().getUserId()))
    ).getId();
    feedDto.getAuthor().setProfileImageUrl(profileImageUrlResolver.resolve(profileId));

    return feedDto;
  }
}

package com.team1.otvoo.feed.elasticsearch.service;

import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeWithDefDto;
import com.team1.otvoo.clothes.entity.Clothes;
import com.team1.otvoo.clothes.entity.ClothesImage;
import com.team1.otvoo.clothes.repository.ClothesAttributeValueRepository;
import com.team1.otvoo.clothes.repository.ClothesImageRepository;
import com.team1.otvoo.clothes.repository.ClothesRepository;
import com.team1.otvoo.feed.elasticsearch.document.FeedDocument;
import com.team1.otvoo.feed.entity.Feed;
import com.team1.otvoo.feed.repository.FeedRepository;
import com.team1.otvoo.recommendation.dto.ElasticOotdDto;
import com.team1.otvoo.storage.S3ImageStorage;
import com.team1.otvoo.user.dto.AuthorDto;
import com.team1.otvoo.user.repository.UserRepository;
import com.team1.otvoo.weather.dto.PrecipitationDto;
import com.team1.otvoo.weather.dto.TemperatureDto;
import com.team1.otvoo.weather.dto.WeatherSummaryDto;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeedMigrationService {
  private final FeedRepository feedRepository;
  private final UserRepository userRepository;
  private final ClothesRepository clothesRepository;
  private final ClothesImageRepository clothesImageRepository;
  private final ClothesAttributeValueRepository clothesAttributeValueRepository;
  private final S3ImageStorage s3ImageStorage;
  private final ElasticsearchOperations elasticsearchOperations;

  public void migrateAllFeedsToElasticsearch() {
    int pageSize = 5000;
    int page = 0;

    while (true) {
      Page<Feed> feedPage = feedRepository.findAll(PageRequest.of(page, pageSize));
      if (feedPage.isEmpty()) {
        break;
      }

      // 1. 엔티티 → Document 변환
      List<FeedDocument> documents = feedPage.getContent().stream()
          .map((Feed feed) -> toDocument(feed,
              feed.getFeedClothes().stream().map(fc -> fc.getClothes().getId()).toList()))
          .toList();

      // 2. Bulk 인덱싱
      bulkIndexFeeds(documents);

      page++;
    }
  }

  private void bulkIndexFeeds(List<FeedDocument> feeds) {
    List<IndexQuery> queries = feeds.stream()
        .map(feed -> new IndexQueryBuilder()
            .withId(feed.getFeedId().toString())
            .withObject(feed)
            .build())
        .toList();

    elasticsearchOperations.bulkIndex(queries, IndexCoordinates.of("feed_index"));
  }

  private FeedDocument toDocument(Feed feed, List<UUID> clothesIds) {
    AuthorDto authorDto = userRepository.projectionAuthorDtoById(feed.getUser().getId());
    List<Clothes> clothes = clothesRepository.findByIdIn(clothesIds);
    List<ElasticOotdDto> ootdDtoList = clothes.stream()
        .map(cl -> {
          ClothesImage clothesImage = clothesImageRepository.findByClothes_Id(cl.getId())
              .orElse(null);
          return ElasticOotdDto.builder()
              .name(cl.getName())
              .type(cl.getType())
              .clothesId(cl.getId())
              .imageKey(clothesImage != null ? clothesImage.getImageKey() : null)
              .contentType(clothesImage != null ? clothesImage.getContentType() : null)
              .attributes(cl.getSelectedValues().stream()
                  .map(sv -> new ClothesAttributeWithDefDto(
                      sv.getDefinition().getId(),
                      sv.getDefinition().getName(),
                      clothesAttributeValueRepository.findByDefinitionId(sv.getDefinition().getId())
                          .stream().map(ca -> ca.getValue()).toList(),
                      sv.getValue().getValue()
                      ))
                  .toList())
              .build();
        })
        .toList();


    return FeedDocument.builder()
        .feedId(feed.getId().toString())
        .content(feed.getContent())
        .createdAt(feed.getCreatedAt() != null ? feed.getCreatedAt().toEpochMilli() : null)
        .updatedAt(feed.getUpdatedAt() != null ? feed.getUpdatedAt().toEpochMilli() : null)
        .likeCount(feed.getLikeCount())
        .weather(new WeatherSummaryDto(
            feed.getWeather().getId(),
            feed.getWeather().getSkyStatus(),
            new PrecipitationDto(
                feed.getWeather().getPrecipitation().getType(),
                feed.getWeather().getPrecipitation().getAmount(),
                feed.getWeather().getPrecipitation().getProbability()),
            new TemperatureDto(
                feed.getWeather().getTemperature().getCurrent(),
                feed.getWeather().getTemperature().getComparedToDayBefore() == null ? null : feed.getWeather().getTemperature().getComparedToDayBefore(),
                feed.getWeather().getTemperature().getMin(),
                feed.getWeather().getTemperature().getMax()
            )))
        .author(authorDto)
        .ootds(ootdDtoList)
        .build();
  }
}

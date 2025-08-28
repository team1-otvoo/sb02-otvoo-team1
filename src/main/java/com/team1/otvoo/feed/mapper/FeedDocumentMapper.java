package com.team1.otvoo.feed.mapper;

import com.team1.otvoo.clothes.dto.OotdDto;
import com.team1.otvoo.clothes.entity.ClothesImage;
import com.team1.otvoo.clothes.repository.ClothesImageRepository;
import com.team1.otvoo.feed.dto.FeedDto;
import com.team1.otvoo.feed.elasticsearch.document.FeedDocument;
import com.team1.otvoo.recommendation.dto.ElasticOotdDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FeedDocumentMapper {
  private final ClothesImageRepository clothesImageRepository;

  public FeedDocument toDocument(FeedDto feedDto) {
    return FeedDocument.builder()
        .feedId(feedDto.getId().toString())
        .createdAt(feedDto.getCreatedAt().toEpochMilli())
        .updatedAt(feedDto.getUpdatedAt() == null ? null : feedDto.getUpdatedAt().toEpochMilli())
        .likeCount(feedDto.getLikeCount())
        .content(feedDto.getContent())
        .author(feedDto.getAuthor())
        .weather(feedDto.getWeather())
        .ootds(feedDto.getOotds().stream().map(this::toElasticOotdDto).toList())
        .build();
  }

  private ElasticOotdDto toElasticOotdDto(OotdDto ootdDto) {
    ClothesImage clothesImage = clothesImageRepository.findByClothes_Id(ootdDto.getClothesId()).orElse(null);
    return ElasticOotdDto.builder()
        .clothesId(ootdDto.getClothesId())
        .name(ootdDto.getName())
        .imageKey(clothesImage != null ? clothesImage.getImageKey() : null)
        .contentType(clothesImage != null ? clothesImage.getContentType() : null)
        .type(ootdDto.getType())
        .attributes(ootdDto.getAttributes())
        .build();
  }
}

package com.team1.otvoo.feed.mapper;

import com.team1.otvoo.clothes.dto.OotdDto;
import com.team1.otvoo.clothes.entity.ClothesImage;
import com.team1.otvoo.clothes.repository.ClothesImageRepository;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.feed.dto.FeedDto;
import com.team1.otvoo.feed.elasticsearch.document.FeedDocument;
import com.team1.otvoo.recommendation.dto.ElasticOotdDto;
import java.util.Map;
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
    ClothesImage clothesImage = clothesImageRepository.findByClothes_Id(ootdDto.getClothesId()).orElseThrow(
        () -> new RestException(ErrorCode.CLOTHES_IMAGE_NOT_FOUND, Map.of("clothesId", ootdDto.getClothesId()))
    );
    return ElasticOotdDto.builder()
        .clothesId(ootdDto.getClothesId())
        .name(ootdDto.getName())
        .imageKey(clothesImage.getImageKey())
        .contentType(clothesImage.getContentType())
        .type(ootdDto.getType())
        .attributes(ootdDto.getAttributes())
        .build();
  }
}

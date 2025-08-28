package com.team1.otvoo.feed.controller;

import com.team1.otvoo.feed.dto.FeedCreateRequest;
import com.team1.otvoo.feed.dto.FeedDto;
import com.team1.otvoo.feed.dto.FeedDtoCursorResponse;
import com.team1.otvoo.feed.dto.FeedSearchCondition;
import com.team1.otvoo.feed.dto.FeedUpdateRequest;
import com.team1.otvoo.feed.elasticsearch.service.FeedElasticSearchService;
import com.team1.otvoo.feed.mapper.FeedPageResponseMapper;
import com.team1.otvoo.feed.service.FeedService;
import com.team1.otvoo.weather.entity.PrecipitationType;
import com.team1.otvoo.weather.entity.SkyStatus;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/feeds")
public class FeedController {
  private final FeedService feedService;
  private final FeedElasticSearchService feedElasticSearchService;
  private final FeedPageResponseMapper pageResponseMapper;

  @PostMapping
  public ResponseEntity<FeedDto> create(@RequestBody @Valid  FeedCreateRequest request) {
    log.info("피드 생성 요청 - authorId: {}", request.authorId());

    FeedDto feedDto = feedService.create(request);

    log.info("피드 생성 완료 - authorId: {}, feedId: {}", feedDto.getAuthor().getName(), feedDto.getId());

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(feedDto);
  }

  @GetMapping
  public ResponseEntity<FeedDtoCursorResponse> getFeeds(
      @RequestParam(name = "cursor", required = false) String cursor,
      @RequestParam(name = "idAfter", required = false) UUID idAfter,
      @RequestParam(name = "limit", defaultValue = "3") int limit,
      @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
      @RequestParam(name = "sortDirection", defaultValue = "DESCENDING") String sortDirection,
      @RequestParam(name = "keywordLike", required = false) String keywordLike,
      @RequestParam(name = "skyStatusEqual", required = false) SkyStatus skyStatusEqual,
      @RequestParam(name = "precipitationTypeEqual", required = false) PrecipitationType precipitationTypeEqual,
      @RequestParam(name = "authorIdEqual", required = false) UUID authorIdEqual) {

    FeedSearchCondition searchCondition = FeedSearchCondition.builder()
        .precipitationTypeEqual(precipitationTypeEqual)
        .authorIdEqual(authorIdEqual)
        .cursor(cursor)
        .idAfter(idAfter)
        .keywordLike(keywordLike)
        .limit(limit)
        .skyStatusEqual(skyStatusEqual)
        .sortBy(sortBy)
        .sortDirection(sortDirection)
        .build();

    // Slice<FeedDto> feedDtoList = feedService.getFeedsWithCursor(searchCondition);
    Slice<FeedDto> feedDtoList = feedElasticSearchService.getFeedsWithCursor(searchCondition);

    return ResponseEntity.ok()
        .body(pageResponseMapper.toPageResponse(feedDtoList, searchCondition));
  }

  @PatchMapping("/{feedId}")
  public ResponseEntity<FeedDto> update(@PathVariable UUID feedId,
      @RequestBody @Valid FeedUpdateRequest request) {
    log.info("피드 수정 요청 - feedId: {}", feedId);

    FeedDto feedDto = feedService.update(feedId, request);

    log.info("피드 수정 완료 - feedId: {}", feedId);
    return ResponseEntity.ok(feedDto);
  }

  @DeleteMapping("/{feedId}")
  public ResponseEntity<Void> delete(@PathVariable UUID feedId) {
    log.info("피드 삭제 요청 - feedId: {}", feedId);

    feedService.delete(feedId);

    log.info("피드 삭제 완료 - feedId: {}", feedId);
    return ResponseEntity.noContent().build();
  }
}
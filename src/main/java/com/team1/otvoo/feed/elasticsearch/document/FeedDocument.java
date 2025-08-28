package com.team1.otvoo.feed.elasticsearch.document;

import com.team1.otvoo.recommendation.dto.ElasticOotdDto;
import com.team1.otvoo.user.dto.AuthorDto;
import com.team1.otvoo.weather.dto.WeatherSummaryDto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = "feed_index")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class FeedDocument {
  @Id
  private String feedId;
  private Long createdAt;
  private Long updatedAt;
  private long likeCount;
  private String content;
  private AuthorDto author;
  private WeatherSummaryDto weather;
  private List<ElasticOotdDto> ootds;
}

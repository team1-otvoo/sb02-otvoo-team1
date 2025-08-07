package com.team1.otvoo.feed.repository;

import com.team1.otvoo.common.AbstractPostgresTest;
import com.team1.otvoo.config.QueryDslConfig;
import com.team1.otvoo.feed.dto.FeedDto;
import com.team1.otvoo.feed.dto.FeedSearchCondition;
import com.team1.otvoo.feed.entity.Feed;
import com.team1.otvoo.user.entity.Profile;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.UserRepository;
import com.team1.otvoo.weather.entity.PrecipitationType;
import com.team1.otvoo.weather.entity.SkyStatus;
import com.team1.otvoo.weather.entity.WeatherForecast;
import com.team1.otvoo.weather.entity.WeatherPrecipitation;
import com.team1.otvoo.weather.repository.WeatherForecastRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Slice;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@DataJpaTest
@Import(QueryDslConfig.class)
public class FeedRepositoryCustomImplTest extends AbstractPostgresTest {
  @Autowired
  private FeedRepository feedRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private WeatherForecastRepository weatherForecastRepository;

  @Autowired
  private EntityManager entityManager;

  @BeforeEach
  void setUp() {
    User author = User.builder()
        .email("test@test.com")
        .password("test1234!")
        .build();
    userRepository.save(author);

    WeatherForecast weatherForecast1 = new WeatherForecast(Instant.now(), Instant.now(), SkyStatus.CLEAR);
    WeatherPrecipitation weatherPrecipitation1 = new WeatherPrecipitation(weatherForecast1, PrecipitationType.NONE, 3.0, 3.0);
    weatherForecast1.setPrecipitation(weatherPrecipitation1);

    WeatherForecast weatherForecast2 = new WeatherForecast(Instant.now(), Instant.now(), SkyStatus.CLOUDY);
    WeatherPrecipitation weatherPrecipitation2 = new WeatherPrecipitation(weatherForecast2, PrecipitationType.RAIN, 3.0, 3.0);
    weatherForecast2.setPrecipitation(weatherPrecipitation2);

    weatherForecastRepository.saveAll(List.of(weatherForecast1, weatherForecast2));

    Feed feed1 = Feed.builder()
        .content("맑을 때 입는 옷1")
        .weatherForecast(weatherForecast1)
        .user(author)
        .build();
    ReflectionTestUtils.setField(feed1, "likeCount", 2);
    ReflectionTestUtils.setField(feed1, "createdAt", Instant.parse("2024-12-21T10:00:00Z"));

    Feed feed2 = Feed.builder()
        .content("맑을 때 입는 옷2")
        .weatherForecast(weatherForecast1)
        .user(author)
        .build();
    ReflectionTestUtils.setField(feed2, "likeCount", 3);
    ReflectionTestUtils.setField(feed2, "createdAt", Instant.parse("2024-12-20T10:00:00Z"));

    Feed feed3 = Feed.builder()
        .content("비올 때 입는 옷1")
        .weatherForecast(weatherForecast2)
        .user(author)
        .build();
    ReflectionTestUtils.setField(feed3, "likeCount", 4);
    ReflectionTestUtils.setField(feed3, "createdAt", Instant.parse("2024-12-19T10:00:00Z"));

    Feed feed4 = Feed.builder()
        .content("비올 때 입는 옷2")
        .weatherForecast(weatherForecast2)
        .user(author)
        .build();
    ReflectionTestUtils.setField(feed4, "likeCount", 4);
    ReflectionTestUtils.setField(feed4, "createdAt", Instant.parse("2024-12-18T10:00:00Z"));

    Feed feed5 = Feed.builder()
        .content("그냥 입는 바지")
        .weatherForecast(weatherForecast1)
        .user(author)
        .build();
    ReflectionTestUtils.setField(feed5, "likeCount", 1);
    ReflectionTestUtils.setField(feed5, "createdAt", Instant.parse("2024-12-27T10:00:00Z"));

    Feed feed6 = Feed.builder()
        .content("맑을 때 입는 옷3")
        .weatherForecast(weatherForecast1)
        .user(author)
        .build();
    ReflectionTestUtils.setField(feed6, "likeCount", 4);
    ReflectionTestUtils.setField(feed6, "createdAt", Instant.parse("2024-12-17T10:00:00Z"));

    feedRepository.saveAll(List.of(feed1, feed2, feed3, feed4, feed5, feed6));

    entityManager.flush();
    entityManager.clear();
  }

  @Test
  @DisplayName("키워드 포함 검색 - likeCount DESC 정렬")
  void searchByCondition_withKeyword_orderBy_likeCount_DESC() {
    // given
    FeedSearchCondition condition = FeedSearchCondition.builder()
        .keywordLike("옷")
        .limit(4)
        .cursor(null)
        .idAfter(null)
        .skyStatusEqual(null)
        .precipitationTypeEqual(null)
        .sortBy("likeCount")
        .sortDirection("DESCENDING")
        .authorIdEqual(null)
        .build();

    // when
    Slice<FeedDto> result = feedRepository.searchByCondition(condition);

    // then
    assertThat(result.getContent().size()).isEqualTo(4);
    assertThat(result.getContent().get(0).getContent()).isEqualTo("비올 때 입는 옷1");
    assertThat(result.getContent().get(1).getContent()).isEqualTo("비올 때 입는 옷2");
    assertThat(result.getContent().get(3).getContent()).isEqualTo("맑을 때 입는 옷2");
    assertThat(result.getContent().get(3).getWeather().precipitation().type()).isEqualTo(PrecipitationType.NONE);
  }

  @Test
  @DisplayName("키워드 + 날씨 + 강수 검색 - createdAt DESC 정렬")
  void searchByCondition_withKeyword_skyStatus_precipitation_orderBy_createdAt_DESC() {
    // given
    FeedSearchCondition condition = FeedSearchCondition.builder()
        .keywordLike("옷")
        .limit(4)
        .cursor(null)
        .idAfter(null)
        .skyStatusEqual(SkyStatus.CLEAR)
        .precipitationTypeEqual(PrecipitationType.NONE)
        .sortBy("createdAt")
        .sortDirection("DESCENDING")
        .authorIdEqual(null)
        .build();

    // when
    Slice<FeedDto> result = feedRepository.searchByCondition(condition);

    // then
    assertThat(result.getContent().size()).isEqualTo(3);
    assertThat(result.getContent().get(0).getContent()).isEqualTo("맑을 때 입는 옷1");
    assertThat(result.getContent().get(1).getContent()).isEqualTo("맑을 때 입는 옷2");
    assertThat(result.getContent().get(2).getContent()).isEqualTo("맑을 때 입는 옷3");
    assertThat(result.getContent().get(2).getWeather().precipitation().type()).isEqualTo(PrecipitationType.NONE);
  }

  @Test
  @DisplayName("커서 포함 키워드 + 날씨 + 강수 검색 - likeCount DESC 정렬")
  void searchByCondition_withCursor_withKeyword_skyStatus_precipitation_orderBy_likeCount_DESC() {
    // given
    FeedSearchCondition condition = FeedSearchCondition.builder()
        .keywordLike("옷")
        .limit(4)
        .cursor("4_2024-12-19T10:00:00Z")
        .idAfter(null)
        .skyStatusEqual(SkyStatus.CLEAR)
        .precipitationTypeEqual(PrecipitationType.NONE)
        .sortBy("likeCount")
        .sortDirection("DESCENDING")
        .authorIdEqual(null)
        .build();

    // when
    Slice<FeedDto> result = feedRepository.searchByCondition(condition);

    // then
    assertThat(result.getContent().size()).isEqualTo(3);
    assertThat(result.getContent().get(0).getContent()).isEqualTo("맑을 때 입는 옷3");
    assertThat(result.getContent().get(1).getContent()).isEqualTo("맑을 때 입는 옷2");
    assertThat(result.getContent().get(2).getContent()).isEqualTo("맑을 때 입는 옷1");
    assertThat(result.getContent().get(2).getWeather().precipitation().type()).isEqualTo(PrecipitationType.NONE);
  }

  @Test
  @DisplayName("커서 포함 키워드 + 날씨 + 강수 검색 - createdAt DESC 정렬")
  void searchByCondition_withCursor_withKeyword_skyStatus_precipitation_orderBy_createdAt_DESC() {
    // given
    FeedSearchCondition condition = FeedSearchCondition.builder()
        .keywordLike("옷")
        .limit(4)
        .cursor("2024-12-19T10:00:00Z")
        .idAfter(null)
        .skyStatusEqual(SkyStatus.CLEAR)
        .precipitationTypeEqual(PrecipitationType.NONE)
        .sortBy("createdAt")
        .sortDirection("DESCENDING")
        .authorIdEqual(null)
        .build();

    // when
    Slice<FeedDto> result = feedRepository.searchByCondition(condition);

    // then
    assertThat(result.getContent().size()).isEqualTo(1);
    assertThat(result.getContent().get(0).getContent()).isEqualTo("맑을 때 입는 옷3");
    assertThat(result.getContent().get(0).getWeather().precipitation().type()).isEqualTo(PrecipitationType.NONE);
  }
  
  @Test
  @DisplayName("마지막 페이지가 아닌 경우 hasNext가 true")
  void searchByCondition_not_lastPage_hasNext_true() {
    // given
    FeedSearchCondition condition = FeedSearchCondition.builder()
        .limit(4)
        .sortBy("createdAt")
        .sortDirection("DESCENDING")
        .build();
      
    // when
    Slice<FeedDto> result = feedRepository.searchByCondition(condition);
      
    // then
    assertThat(result.hasNext()).isTrue();
  }

  @Test
  @DisplayName("마지막 페이지인 경우 hasNext가 false")
  void searchByCondition_lastPage_hasNext_false() {
    // given
    FeedSearchCondition condition = FeedSearchCondition.builder()
        .limit(6)
        .sortBy("createdAt")
        .sortDirection("DESCENDING")
        .build();

    // when
    Slice<FeedDto> result = feedRepository.searchByCondition(condition);

    // then
    assertThat(result.hasNext()).isFalse();
  }

  @Test
  @DisplayName("검색 결과가 없는 경우")
  void searchByCondition_emptyResult() {
    // given
    FeedSearchCondition condition = FeedSearchCondition.builder()
        .keywordLike("결과없음")
        .limit(4)
        .sortBy("createdAt")
        .sortDirection("DESCENDING")
        .authorIdEqual(null)
        .build();

    // when
    Slice<FeedDto> result = feedRepository.searchByCondition(condition);

    // then
    assertThat(result).isEmpty();
  }
}

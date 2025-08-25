package com.team1.otvoo.feed.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team1.otvoo.feed.dto.FeedDto;
import com.team1.otvoo.feed.dto.FeedSearchCondition;
import com.team1.otvoo.feed.entity.QFeed;
import com.team1.otvoo.feed.entity.QFeedLike;
import com.team1.otvoo.user.dto.AuthorDto;
import com.team1.otvoo.user.entity.QProfile;
import com.team1.otvoo.user.entity.QProfileImage;
import com.team1.otvoo.user.entity.QUser;
import com.team1.otvoo.weather.dto.PrecipitationDto;
import com.team1.otvoo.weather.dto.TemperatureDto;
import com.team1.otvoo.weather.dto.WeatherSummaryDto;
import com.team1.otvoo.weather.entity.PrecipitationType;
import com.team1.otvoo.weather.entity.QWeatherForecast;
import com.team1.otvoo.weather.entity.QWeatherPrecipitation;
import com.team1.otvoo.weather.entity.QWeatherTemperature;
import com.team1.otvoo.weather.entity.SkyStatus;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class FeedRepositoryCustomImpl implements FeedRepositoryCustom {

  private final JPAQueryFactory queryFactory;
  QFeed feed = QFeed.feed;
  QUser user = QUser.user;
  QProfile profile = QProfile.profile;
  QProfileImage profileImage = QProfileImage.profileImage;
  QWeatherForecast weather = QWeatherForecast.weatherForecast;
  QWeatherPrecipitation precipitation = QWeatherPrecipitation.weatherPrecipitation;
  QWeatherTemperature temperature = QWeatherTemperature.weatherTemperature;
  QFeedLike feedLike = QFeedLike.feedLike;

  @Override
  public Slice<FeedDto> searchByCondition(FeedSearchCondition condition, UUID currentUserId) {
    BooleanBuilder where = new BooleanBuilder();
    where.and(keywordLike(condition.keywordLike()))
        .and(precipitationTypeEq(condition.precipitationTypeEqual()))
        .and(skyStatusEq(condition.skyStatusEqual()))
        .and(authorIdEq(condition.authorIdEqual()));

    BooleanExpression cursorExpression = cursorCondition(
        condition.cursor(),
        condition.idAfter(),
        condition.sortBy(),
        condition.sortDirection()
    );

    if (cursorExpression != null) {
      where.and(cursorExpression);
    }

    OrderSpecifier<?>[] orderSpecifiers = createdOrderSpecifier(
        condition.sortBy(),
        condition.sortDirection()
    );

    int limit = condition.limit();

    List<FeedDto> results = queryFactory
        .select(Projections.constructor(
            FeedDto.class,
            feed.id,
            feed.createdAt,
            feed.updatedAt,
            Projections.constructor(
                AuthorDto.class,
                user.id,
                profile.name,
                Expressions.nullExpression(String.class)
            ),
            Projections.constructor(
                WeatherSummaryDto.class,
                weather.id,
                weather.skyStatus,
                Projections.constructor(
                    PrecipitationDto.class,
                    precipitation.type,
                    precipitation.amount,
                    precipitation.probability
                ),
                Projections.constructor(
                    TemperatureDto.class,
                    temperature.current,
                    temperature.comparedToDayBefore,
                    temperature.min,
                    temperature.max
                )
            ),
            // Ootd List는 Service단에서 따로 조립
            Expressions.constant(Collections.emptyList()),
            feed.content,
            feed.likeCount,
            feed.commentCount,
            feedLike.id.isNotNull()
        ))
        .from(feed)
        .leftJoin(feedLike)
        .on(feedLike.feed.id.eq(feed.id).and(feedLike.likedBy.id.eq(currentUserId)))
        .join(feed.user, user)
        .leftJoin(profile).on(profile.user.id.eq(user.id))
        .leftJoin(profileImage).on(profileImage.profile.id.eq(profile.id))
        .join(feed.weather, weather)
        .leftJoin(precipitation).on(precipitation.forecast.eq(weather))
        .leftJoin(temperature).on(temperature.forecast.eq(weather))
        .where(where)
        .orderBy(orderSpecifiers)
        .limit(limit + 1)
        .fetch();

    boolean hasNext = results.size() > limit;
    if (hasNext) {
      results.remove(results.size() - 1);
    }

    return new SliceImpl<>(results, condition.toPageable(), hasNext);
  }

  private BooleanExpression keywordLike(String keywordLike) {
    return StringUtils.hasText(keywordLike) ?
        feed.content.containsIgnoreCase(keywordLike) : null;
  }

  private BooleanExpression precipitationTypeEq(PrecipitationType type) {
    return type != null ? feed.weather.precipitation.type.eq(type) : null;
  }

  private BooleanExpression skyStatusEq(SkyStatus status) {
    return status != null ? feed.weather.skyStatus.eq(status) : null;
  }

  private BooleanExpression authorIdEq(UUID id) {
    return id != null ? feed.user.id.eq(id) : null;
  }

  // 커서 조건 이후의 값들만 조회
  // 정렬과는 별개지만, 정렬 조건과 맞춰줘야 페이지 간 정렬 정합성이 유지됨
  // ex) 1페이지의 끝 데이터와 2페이지의 첫 데이터 간에도 정렬 정합성이 맞아야함
  private BooleanExpression cursorCondition(String cursor, UUID idAfter, String sortBy,
      String sortDirection) {
    if (!StringUtils.hasText(cursor)) {
      return null;
    }

    boolean descending = "DESCENDING".equalsIgnoreCase(sortDirection);

    if ("likeCount".equalsIgnoreCase(sortBy)) {
      // 복합 커서를 두 커서 조건으로 분리 (복합 커서: likeCount_createdAt)
      String[] cursorList = cursor.split("_");

      long countCursorValue = Long.parseLong(cursorList[0]);
      Instant createdAtCursorValue = Instant.parse(cursorList[1]);

      BooleanExpression mainCondition = descending
          ? feed.likeCount.lt(countCursorValue)
          : feed.likeCount.gt(countCursorValue);

      // likeCount가 같은 경우 최신순 정렬이므로 createdAt lt 고정
      // 그래야 다음 페이지 데이터들의 likeCount가 이전 페이지에서 넘어온 cursor와 같을 경우
      // cursor 보다 오래된 (createdAt이 작은) 데이터가 오게됨 -> 최신순 데이터 페이지를 안전하게 이어갈 수 있음
      BooleanExpression tieCondition = feed.likeCount.eq(countCursorValue)
          .and(feed.createdAt.lt(createdAtCursorValue));

      // createdAt까지 같은 경우 (최후의 보루지만 likeCount와 createdAt까지 같은 경우가 없으므로, 크게 의미 X)
      // likeCount + createdAt까지 같은데, 다음 데이터의 UUID가 cursor보다 큰지, 작은지 어떻게 알 수 있지?
      // -> 알 수 없음. id.lt로 가져오면 만약 UUID가 클 경우 데이터가 누락됨
      // id.gt로 가져오면 UUID가 작을 경우 데이터가 누락됨
      // UUIDv4로는 매우 희박한 상황에서 발생하는 예외까지 고려하는 완벽한 페이지네이션이 불가능하다.
      // 일단은 변경 비용이 크고, 발생할 확률이 로또보다 작으므로 이렇게 구현해두고, 추후 Postgre18의 UUIDv7이나 Long값으로 ID를 사용하기
      BooleanExpression lastCondition = idAfter != null
          ? feed.likeCount.eq(countCursorValue)
          .and(feed.createdAt.eq(createdAtCursorValue))
          .and(feed.id.lt(idAfter))
          : null;

      BooleanExpression combinedCondition = mainCondition.or(tieCondition);
      if (lastCondition != null) {
        combinedCondition = combinedCondition.or(lastCondition);
      }

      return combinedCondition;
    }
    // sortBy가 createdAt인 경우
    else {
      Instant createdAtCursorValue = Instant.parse(cursor);

      BooleanExpression mainCondition = descending
          ? feed.createdAt.lt(createdAtCursorValue)
          : feed.createdAt.gt(createdAtCursorValue);

      BooleanExpression tieCondition = idAfter != null
          ? feed.createdAt.eq(createdAtCursorValue)
          .and(feed.id.lt(idAfter))
          : null;

      BooleanExpression combinedCondition = mainCondition;
      if (tieCondition != null) {
        combinedCondition = combinedCondition.or(tieCondition);
      }

      return combinedCondition;
    }
  }

  // 정렬 조건 (같은 경우엔 createdAt을 기준으로 내림차순 최신순 정렬)
  private OrderSpecifier<?>[] createdOrderSpecifier(String sortBy, String sortDirection) {
    Order direction = "DESCENDING".equalsIgnoreCase(sortDirection)
        ? Order.DESC
        : Order.ASC;

    return "likeCount".equalsIgnoreCase(sortBy)
        ? new OrderSpecifier[]{
        new OrderSpecifier<>(direction, feed.likeCount),
        new OrderSpecifier<>(Order.DESC, feed.createdAt),
        new OrderSpecifier<>(Order.DESC, feed.id)
    }
        : new OrderSpecifier[]{
            new OrderSpecifier<>(direction, feed.createdAt),
            new OrderSpecifier<>(Order.DESC, feed.id)
        };
  }
}
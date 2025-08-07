package com.team1.otvoo.clothes.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team1.otvoo.clothes.dto.SortBy;
import com.team1.otvoo.clothes.dto.SortDirection;
import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeDefSearchCondition;
import com.team1.otvoo.clothes.entity.ClothesAttributeDefinition;
import com.team1.otvoo.clothes.entity.QClothesAttributeDefinition;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ClothesAttributeDefRepositoryCustomImpl implements
    ClothesAttributeDefRepositoryCustom {

  private final JPAQueryFactory queryFactory;
  private final QClothesAttributeDefinition definition = QClothesAttributeDefinition.clothesAttributeDefinition;

  @Override
  public List<ClothesAttributeDefinition> searchWithCursor(
      ClothesAttributeDefSearchCondition condition) {

    BooleanBuilder builder = buildKeywordCondition(condition);

    if (condition.cursor() != null) {
      builder.and(buildCursorCondition(condition));
    }

    return queryFactory
        .selectFrom(definition)
        .where(builder)
        .orderBy(createOrderSpecifiers(condition))
        .limit(condition.limit() + 1)
        .fetch();
  }

  @Override
  public long countWithCondition(ClothesAttributeDefSearchCondition condition) {
    BooleanBuilder builder = buildKeywordCondition(condition);

    Long count = queryFactory
        .select(definition.count())
        .from(definition)
        .where(builder)
        .fetchOne();

    return count != null ? count : 0L;
  }

  private BooleanBuilder buildKeywordCondition(ClothesAttributeDefSearchCondition condition) {
    BooleanBuilder builder = new BooleanBuilder();

    if (condition.keywordLike() != null && !condition.keywordLike().isBlank()) {
      builder.and(definition.name.containsIgnoreCase(condition.keywordLike()));
    }
    return builder;
  }

  private BooleanExpression buildCursorCondition(ClothesAttributeDefSearchCondition condition) {
    SortBy sortBy = condition.sortBy();
    SortDirection direction = condition.sortDirection();

    return switch (sortBy) {
      case NAME -> {
        String cursorValue = condition.cursor();
        BooleanExpression baseCondition = (direction == SortDirection.ASCENDING)
            ? definition.name.gt(cursorValue)
            : definition.name.lt(cursorValue);

        if (condition.idAfter() != null) {
          BooleanExpression tieCondition = (direction == SortDirection.ASCENDING)
              ? definition.name.eq(cursorValue).and(definition.id.gt(condition.idAfter()))
              : definition.name.eq(cursorValue).and(definition.id.lt(condition.idAfter()));
          yield baseCondition.or(tieCondition);
        }
        yield baseCondition;
      }

      case CREATED_AT -> {
        Instant cursorTime = Instant.parse(condition.cursor());
        BooleanExpression baseCondition = (direction == SortDirection.ASCENDING)
            ? definition.createdAt.gt(cursorTime)
            : definition.createdAt.lt(cursorTime);

        if (condition.idAfter() != null) {
          BooleanExpression tieCondition = (direction == SortDirection.ASCENDING)
              ? definition.createdAt.eq(cursorTime).and(definition.id.gt(condition.idAfter()))
              : definition.createdAt.eq(cursorTime).and(definition.id.lt(condition.idAfter()));
          yield baseCondition.or(tieCondition);
        }
        yield baseCondition;
      }
    };
  }

  private OrderSpecifier<?>[] createOrderSpecifiers(ClothesAttributeDefSearchCondition condition) {
    SortBy sortBy = condition.sortBy();
    Order direction = condition.sortDirection() == SortDirection.ASCENDING ? Order.ASC : Order.DESC;

    return switch (sortBy) {
      case NAME -> new OrderSpecifier[]{
          new OrderSpecifier<>(direction, definition.name),
          new OrderSpecifier<>(direction, definition.id),
      };
      case CREATED_AT -> new OrderSpecifier[]{
          new OrderSpecifier<>(direction, definition.createdAt),
          new OrderSpecifier<>(direction, definition.id),
      };
    };
  }
}
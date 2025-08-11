package com.team1.otvoo.clothes.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team1.otvoo.clothes.dto.ClothesSearchCondition;
import com.team1.otvoo.clothes.entity.Clothes;
import com.team1.otvoo.clothes.entity.QClothes;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ClothesRepositoryCustomImpl implements ClothesRepositoryCustom {

  private final JPAQueryFactory queryFactory;
  private final QClothes clothes = QClothes.clothes;

  @Override
  public List<Clothes> searchWithCursor(ClothesSearchCondition condition) {
    BooleanBuilder builder = buildCondition(condition);

    if (condition.cursor() != null && condition.idAfter() != null) {
      builder.and(buildCursorCondition(condition));
    }

    return queryFactory
        .selectFrom(clothes)
        .where(builder)
        .orderBy(createOrderSpecifiers(condition))
        .limit(condition.limit() + 1)
        .fetch();

  }

  @Override
  public long countWithCondition(ClothesSearchCondition condition) {
    BooleanBuilder builder = buildCondition(condition);

    Long count = queryFactory
        .select(clothes.count())
        .from(clothes)
        .where(builder)
        .fetchOne();
    return count != null ? count : 0L;
  }

  private BooleanBuilder buildCondition(ClothesSearchCondition condition) {
    BooleanBuilder builder = new BooleanBuilder();
    if (condition.ownerId() != null) {
      builder.and(clothes.owner.id.eq(condition.ownerId()));
    }
    if (condition.type() != null) {
      builder.and(clothes.type.eq(condition.type()));
    }
    return builder;
  }

  private BooleanBuilder buildCursorCondition(ClothesSearchCondition condition) {
    BooleanBuilder builder = new BooleanBuilder();
    Instant cursorTime = Instant.parse(condition.cursor());
    builder.and(clothes.createdAt.lt(cursorTime)
        .or(clothes.createdAt.eq(cursorTime).and(clothes.id.lt(condition.idAfter()))));
    return builder;
  }

  private OrderSpecifier<?>[] createOrderSpecifiers(ClothesSearchCondition condition) {
    return new OrderSpecifier<?>[]{
        new OrderSpecifier<>(Order.DESC, clothes.createdAt),
        new OrderSpecifier<>(Order.DESC, clothes.id)
    };
  }
}
package com.team1.otvoo.user.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.user.dto.SortBy;
import com.team1.otvoo.user.dto.SortDirection;
import com.team1.otvoo.user.dto.UserDtoCursorRequest;
import com.team1.otvoo.user.dto.UserSlice;
import com.team1.otvoo.user.entity.QUser;
import com.team1.otvoo.user.entity.User;
import jakarta.persistence.EntityManager;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Repository
public class UserQueryDslRepositoryImpl implements UserQueryDslRepository {

  private final JPAQueryFactory queryFactory;

  public UserQueryDslRepositoryImpl(EntityManager em) {
    this.queryFactory = new JPAQueryFactory(em);
  }

  @Override
  public UserSlice searchUsersWithCursor(UserDtoCursorRequest request) {
    QUser user = QUser.user;

    BooleanBuilder builder = buildConditions(user, request);
    OrderSpecifier<?>[] orderSpecifiers = getOrderSpecifiers(user, request);

    int limit = request.limit() > 0 ? request.limit() : 20;

    long totalCount = queryFactory
        .select(user.count())
        .from(user)
        .where(builder)
        .fetchOne();

    List<User> result = queryFactory
        .selectFrom(user)
        .where(builder)
        .orderBy(orderSpecifiers)
        .limit(limit + 1)
        .fetch();

    boolean hasNext = result.size() > limit;
    if (hasNext) {
      result = result.subList(0, limit);
    }

    return new UserSlice(result, hasNext, totalCount);
  }


  private BooleanBuilder buildConditions(QUser user, UserDtoCursorRequest request) {
    BooleanBuilder builder = new BooleanBuilder();

    if (request.emailLike() != null && !request.emailLike().isBlank()) {
      builder.and(user.email.containsIgnoreCase(request.emailLike()));
    }

    if (request.roleEqual() != null) {
      builder.and(user.role.eq(request.roleEqual()));
    }

    if (request.locked() != null) {
      builder.and(user.locked.eq(request.locked()));
    }
    if ((request.cursor() != null && request.idAfter() == null)
        || (request.cursor() == null && request.idAfter() != null)) {

      log.warn("cursor 나 idAfter 둘 중 하나만 null 입니다.");

      throw new RestException(ErrorCode.INVALID_INPUT_VALUE, Map.of("message", "cursor 나 idAfter 둘 중 하나만 null 입니다."));
    }

    if (request.cursor() != null && request.idAfter() != null) {
      builder.and(buildCursorCondition(user, request));
    }

    return builder;
  }

  private BooleanBuilder buildCursorCondition(QUser user, UserDtoCursorRequest request) {
    SortBy sortBy = request.sortBy(); // enum
    SortDirection direction = request.sortDirection();
    UUID idCursor = request.idAfter();

    BooleanBuilder condition = new BooleanBuilder();

    switch (sortBy) {
      case EMAIL -> {
        String cursorValue = request.cursor();
        if (direction == SortDirection.ASCENDING) {
          condition.and(user.email.gt(cursorValue)
              .or(user.email.eq(cursorValue).and(user.id.gt(idCursor))));
        } else {
          condition.and(user.email.lt(cursorValue)
              .or(user.email.eq(cursorValue).and(user.id.lt(idCursor))));
        }
      }

      case CREATED_AT -> {
        Instant cursorTime = Instant.parse(request.cursor());
        if (direction == SortDirection.ASCENDING) {
          condition.and(user.createdAt.gt(cursorTime)
              .or(user.createdAt.eq(cursorTime).and(user.id.gt(idCursor))));
        } else {
          condition.and(user.createdAt.lt(cursorTime)
              .or(user.createdAt.eq(cursorTime).and(user.id.lt(idCursor))));
        }
      }
    }

    return condition;
  }


  private OrderSpecifier<?>[] getOrderSpecifiers(QUser user, UserDtoCursorRequest request) {
    Order direction = request.sortDirection() == SortDirection.ASCENDING ? Order.ASC : Order.DESC;

    return switch (request.sortBy()) {
      case EMAIL -> new OrderSpecifier[]{
          new OrderSpecifier<>(direction, user.email),
          new OrderSpecifier<>(direction, user.id)
      };
      case CREATED_AT -> new OrderSpecifier[]{
          new OrderSpecifier<>(direction, user.createdAt),
          new OrderSpecifier<>(direction, user.id)
      };
    };
  }
}

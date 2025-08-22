package com.team1.otvoo.user.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.user.dto.AuthorDto;
import com.team1.otvoo.user.dto.SortBy;
import com.team1.otvoo.user.dto.SortDirection;
import com.team1.otvoo.user.dto.UserDtoCursorRequest;
import com.team1.otvoo.user.dto.UserRow;
import com.team1.otvoo.user.dto.UserSlice;
import com.team1.otvoo.user.entity.QProfile;
import com.team1.otvoo.user.entity.QProfileImage;
import com.team1.otvoo.user.entity.QUser;
import jakarta.persistence.EntityManager;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Repository
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  public UserRepositoryCustomImpl(EntityManager em) {
    this.queryFactory = new JPAQueryFactory(em);
  }

  @Override
  public AuthorDto projectionAuthorDtoById(UUID userId) {
    QUser user = QUser.user;
    QProfile profile = QProfile.profile;
    QProfileImage profileImage = QProfileImage.profileImage;

    return queryFactory
        .select(Projections.constructor(
            AuthorDto.class,
            user.id,
            profile.name,
            profileImage.objectKey
        ))
        .from(user)
        .leftJoin(profile).on(profile.user.eq(user))
        .leftJoin(profileImage).on(profileImage.profile.eq(profile))
        .where(user.id.eq(userId))
        .fetchOne();
  }

  @Override
  public UserSlice<UserRow> searchUserRowWithCursor(UserDtoCursorRequest request) {
    QUser user = QUser.user;
    QProfile profile = QProfile.profile;
    QProfileImage profileImage = QProfileImage.profileImage;

    BooleanBuilder where = buildConditions(user, request);
    OrderSpecifier<?>[] orders = getOrderSpecifiers(user, request);
    int limit = request.limit() > 0 ? request.limit() : 20;

    // count
    long total = queryFactory.select(user.count())
        .from(user)
        .where(where)
        .fetchOne();

    // rows (한 방 조회)
    List<UserRow> rows = queryFactory
        .select(Projections.constructor(UserRow.class,
            user.id,
            user.createdAt,
            user.email,
            profile.name,
            user.role,
            user.locked
        ))
        .from(user)
        .leftJoin(profile).on(profile.user.eq(user))
        .leftJoin(profileImage).on(profileImage.profile.eq(profile)) // 1:1이므로 row 폭발 없음
        .where(where)
        .orderBy(orders)
        .limit(limit + 1)
        .fetch();

    boolean hasNext = rows.size() > limit;
    if (hasNext) rows = rows.subList(0, limit);

    return new UserSlice<>(rows, hasNext, total);
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

    // cursor 검증
    if ((request.cursor() != null) ^ (request.idAfter() != null)) {
      throw new RestException(ErrorCode.INVALID_INPUT_VALUE,
          Map.of("message","cursor와 idAfter는 함께(null 아님) 또는 함께(null)여야 합니다."));
    }
    if (request.cursor() != null) {
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
        String cursor = request.cursor();
        if (direction == SortDirection.ASCENDING) {
          condition.and(user.email.gt(cursor)
              .or(user.email.eq(cursor).and(user.id.gt(idCursor))));
        } else {
          condition.and(user.email.lt(cursor)
              .or(user.email.eq(cursor).and(user.id.lt(idCursor))));
        }
      }
      case CREATED_AT -> {
        Instant cursor = Instant.parse(request.cursor());
        if (direction == SortDirection.ASCENDING) {
          condition.and(user.createdAt.gt(cursor)
              .or(user.createdAt.eq(cursor).and(user.id.gt(idCursor))));
        } else {
          condition.and(user.createdAt.lt(cursor)
              .or(user.createdAt.eq(cursor).and(user.id.lt(idCursor))));
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

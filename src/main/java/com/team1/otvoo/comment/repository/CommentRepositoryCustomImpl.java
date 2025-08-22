package com.team1.otvoo.comment.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team1.otvoo.comment.dto.CommentCursor;
import com.team1.otvoo.comment.dto.CommentDto;
import com.team1.otvoo.comment.entity.QFeedComment;
import com.team1.otvoo.user.dto.AuthorDto;
import com.team1.otvoo.user.entity.QProfile;
import com.team1.otvoo.user.entity.QProfileImage;
import com.team1.otvoo.user.entity.QUser;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class CommentRepositoryCustomImpl implements CommentRepositoryCustom{
  private final JPAQueryFactory queryFactory;
  QFeedComment comment = QFeedComment.feedComment;
  QUser user = QUser.user;
  QProfile profile = QProfile.profile;
  QProfileImage profileImage = QProfileImage.profileImage;

  @Override
  public Slice<CommentDto> findCommentsWithCursor(CommentCursor commentCursor, UUID feedId) {
    BooleanBuilder where = new BooleanBuilder();

    BooleanExpression cursorExpression = cursorCondition(
        commentCursor.cursor(),
        commentCursor.idAfter()
    );

    where.and(comment.feed.id.eq(feedId));

    if (cursorExpression != null) {
      where.and(cursorExpression);
    }

    int limit = commentCursor.limit();

    OrderSpecifier<?> [] orderSpecifiers = new OrderSpecifier[]{
        new OrderSpecifier<>(Order.ASC, comment.createdAt),
        new OrderSpecifier<>(Order.ASC, comment.id)};

    List<CommentDto> results = queryFactory
        .select(Projections.constructor(
            CommentDto.class,
            comment.id,
            comment.createdAt,
            Expressions.constant(feedId),
            Projections.constructor(
                AuthorDto.class,
                user.id,
                profile.name,
                profileImage.objectKey),
            comment.content))
        .from(comment)
        .join(comment.user, user)
        .leftJoin(profile).on(profile.user.id.eq(user.id))
        .leftJoin(profileImage).on(profileImage.profile.id.eq(profile.id))
        .where(where)
        .orderBy(orderSpecifiers)
        .limit(limit + 1)
        .fetch();

    boolean hasNext = results.size() > limit;

    if(hasNext) {
      results.remove(results.size() - 1);
    }

    return new SliceImpl<>(results, commentCursor.toPageable(), hasNext);
  }

  private BooleanExpression cursorCondition(String cursor, UUID idAfter) {
    if (!StringUtils.hasText(cursor)) {
      return null;
    }

    Instant createdAtCursorValue = Instant.parse(cursor);
    BooleanExpression mainCondition = comment.createdAt.gt(createdAtCursorValue);
    BooleanExpression tieCondition = idAfter != null
          ? comment.createdAt.eq(createdAtCursorValue)
          .and(comment.createdAt.gt(createdAtCursorValue))
          : null;

      return mainCondition.or(tieCondition);
  }

  private OrderSpecifier<?>[] createdOrderSpecifier() {
    return new OrderSpecifier[]{
        new OrderSpecifier<>(Order.ASC, comment.createdAt),
        new OrderSpecifier<>(Order.ASC, comment.id)
    };
  }
}

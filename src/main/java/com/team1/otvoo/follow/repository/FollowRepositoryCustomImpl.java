package com.team1.otvoo.follow.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team1.otvoo.follow.dto.FollowCursorDto;
import com.team1.otvoo.follow.entity.QFollow;
import com.team1.otvoo.user.dto.UserSummary;
import com.team1.otvoo.user.entity.QProfile;
import com.team1.otvoo.user.entity.QProfileImage;
import com.team1.otvoo.user.entity.QUser;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class FollowRepositoryCustomImpl implements FollowRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  private enum SearchType {
    FOLLOWING,
    FOLLOWER
  }

  @Override
  public List<FollowCursorDto> findFollowingsWithCursor(UUID followerId, Instant cursor, UUID idAfter, int limit,
      String nameLike) {
    return findFollowListWithCursor(followerId, cursor, idAfter, limit, nameLike, SearchType.FOLLOWING);
  }

  @Override
  public List<FollowCursorDto> findFollowersWithCursor(UUID followeeId, Instant cursor, UUID idAfter, int limit,
      String nameLike) {
    return findFollowListWithCursor(followeeId, cursor, idAfter, limit, nameLike, SearchType.FOLLOWER);
  }

  private List<FollowCursorDto> findFollowListWithCursor(UUID userId, Instant cursor, UUID idAfter, int limit,
      String nameLike, SearchType type){

    QFollow follow = QFollow.follow;
    QUser userFollowee = new QUser("userFollowee");
    QUser userFollower = new QUser("userFollower");

    QProfile profileFollowee = new QProfile("profileFollowee");
    QProfile profileFollower = new QProfile("profileFollower");

    QProfileImage imageFollowee = new QProfileImage("imageFollowee");
    QProfileImage imageFollower = new QProfileImage("imageFollower");

    BooleanExpression userIdCondition;
    if (type == SearchType.FOLLOWING) {
      // followee 목록 조회
      userIdCondition = follow.follower.id.eq(userId);
    } else {
      // follower 목록 조회
      userIdCondition = follow.followee.id.eq(userId);
    }

    return queryFactory
        .select(Projections.constructor(FollowCursorDto.class,
            follow.id,
            follow.createdAt,
            Projections.constructor(UserSummary.class,
                userFollowee.id,
                profileFollowee.name,
                imageFollowee.objectKey
            ),
            Projections.constructor(UserSummary.class,
                userFollower.id,
                profileFollower.name,
                imageFollower.objectKey
            )
        ))
        .from(follow)
        .join(userFollowee).on(userFollowee.id.eq(follow.followee.id))
        .join(userFollower).on(userFollower.id.eq(follow.follower.id))

        // followee 쪽 profile + image
        .leftJoin(profileFollowee).on(profileFollowee.user.id.eq(userFollowee.id))
        .leftJoin(imageFollowee).on(imageFollowee.profile.id.eq(profileFollowee.id))

        // follower 쪽 profile + image
        .leftJoin(profileFollower).on(profileFollower.user.id.eq(userFollower.id))
        .leftJoin(imageFollower).on(imageFollower.profile.id.eq(profileFollower.id))

        .where(
            userIdCondition,
            nameLikeCondition(type, profileFollowee, profileFollower, nameLike),
            cursorCondition(follow, cursor, idAfter)
        )
        .orderBy(follow.createdAt.desc(), follow.id.desc())
        .limit(limit)
        .fetch();
  }

  // 사용자 이름 검색 조건
  private BooleanExpression nameLikeCondition(SearchType type, QProfile profileFollowee, QProfile profileFollower, String nameLike) {
    if (!StringUtils.hasText(nameLike)) {
      return null;
    }
    if (type == SearchType.FOLLOWING) {
      return profileFollowee.name.containsIgnoreCase(nameLike);
    } else {
      return profileFollower.name.containsIgnoreCase(nameLike);
    }
  }

  // 커서 조건
  private BooleanExpression cursorCondition(QFollow follow, Instant cursor, UUID idAfter) {
    if (cursor == null || idAfter == null) {
      return null;
    }
    return follow.createdAt.lt(cursor)
        .or(follow.createdAt.eq(cursor).and(follow.id.lt(idAfter))); // idAfter 어떻게 할지
  }
}




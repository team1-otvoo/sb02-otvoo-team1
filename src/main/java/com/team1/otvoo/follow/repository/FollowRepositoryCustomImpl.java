package com.team1.otvoo.follow.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team1.otvoo.follow.entity.Follow;
import com.team1.otvoo.follow.entity.QFollow;
import com.team1.otvoo.user.entity.QProfile;
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
  public List<Follow> findFollowingsWithCursor(UUID followerId, Instant cursor, UUID idAfter, int limit,
      String nameLike) {
    return findFollowListWithCursor(followerId, cursor, idAfter, limit, nameLike, SearchType.FOLLOWING);
  }

  @Override
  public List<Follow> findFollowersWithCursor(UUID followeeId, Instant cursor, UUID idAfter, int limit,
      String nameLike) {
    return findFollowListWithCursor(followeeId, cursor, idAfter, limit, nameLike, SearchType.FOLLOWER);
  }

  private List<Follow> findFollowListWithCursor(UUID userId, Instant cursor, UUID idAfter, int limit,
      String nameLike, SearchType type){

    QFollow follow = QFollow.follow;
    QUser userToJoin;
    QProfile profile = QProfile.profile;
    BooleanExpression userIdCondition;

    if (type == SearchType.FOLLOWING) {
      // followee 목록 조회
      userToJoin = follow.followee;
      userIdCondition = follow.follower.id.eq(userId);
    } else {
      // follower 목록 조회
      userToJoin = follow.follower;
      userIdCondition = follow.followee.id.eq(userId);
    }

    return queryFactory
        .selectFrom(follow)
        .join(userToJoin).fetchJoin()
        .leftJoin(profile).on(profile.user.id.eq(userToJoin.id)).fetchJoin()
        .where(
            userIdCondition, // ID 조건
            nameLike(profile, nameLike), // 사용자 이름 검색 조건
            cursorCondition(follow, cursor, idAfter) // 커서 조건
        )
        .orderBy(follow.createdAt.desc(), follow.id.desc())
        .limit(limit)
        .fetch();
  }

  // 사용자 이름 검색 조건
  private BooleanExpression nameLike(QProfile profile, String nameLike) {
    if (!StringUtils.hasText(nameLike)) {
      return null;
    }
    return profile.name.containsIgnoreCase(nameLike);
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




package com.team1.otvoo.notification.repository;

import static com.team1.otvoo.notification.entity.QNotification.notification;
import static com.team1.otvoo.notification.entity.QNotificationReadStatus.notificationReadStatus;
import static com.team1.otvoo.user.entity.QUser.user;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team1.otvoo.notification.entity.Notification;
import com.team1.otvoo.notification.entity.QNotification;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryCustomImpl implements NotificationRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<Notification> findUnreadNotificationsWithCursor(UUID receiverId, Instant cursor, UUID idAfter, int limit) {
    Instant userCreatedAt = queryFactory
        .select(user.createdAt)
        .from(user)
        .where(user.id.eq(receiverId))
        .fetchOne();

    if (userCreatedAt == null) {
      // 안전하게 처리: 해당 유저가 없으면 빈 리스트 반환
      return List.of();
    }

    return queryFactory
        .selectFrom(notification)
        .leftJoin(notification.receiver).fetchJoin()
        .leftJoin(notificationReadStatus)
        .on(
            notificationReadStatus.notification.id.eq(notification.id),
            notificationReadStatus.user.id.eq(receiverId)
        )
        .where(
            notification.receiver.id.eq(receiverId)
                .or(
                    notification.receiver.id.isNull()
                        .and(notificationReadStatus.id.isNull())
                        .and(notification.createdAt.goe(userCreatedAt)) // 가입 이후 broadcast 필터링
                ),
            cursorCondition(notification, cursor, idAfter) // 커서 조건
        )
        .orderBy(notification.createdAt.desc(), notification.id.desc())
        .limit(limit)
        .fetch();
  }

  @Override
  public long countUnreadNotifications(UUID receiverId) {
    Instant userCreatedAt = queryFactory
        .select(user.createdAt)
        .from(user)
        .where(user.id.eq(receiverId))
        .fetchOne();

    if (userCreatedAt == null) {
      return 0L;
    }

    Long count = queryFactory
        .select(notification.count())
        .from(notification)
        .leftJoin(notificationReadStatus)
        .on(
            notificationReadStatus.notification.id.eq(notification.id),
            notificationReadStatus.user.id.eq(receiverId)
        )
        .where(
            notification.receiver.id.eq(receiverId)
                .or(
                    notification.receiver.id.isNull()
                        .and(notificationReadStatus.id.isNull())
                        .and(notification.createdAt.goe(userCreatedAt))
                )
        )
        .fetchOne();

    return count != null ? count : 0L;
  }

  // 커서 조건
  private BooleanExpression cursorCondition(QNotification notification, Instant cursor, UUID idAfter) {
    if (cursor == null || idAfter == null) {
      return null;
    }
    return notification.createdAt.lt(cursor)
        .or(notification.createdAt.eq(cursor).and(notification.id.lt(idAfter))); // idAfter 어떻게 할지
  }
}

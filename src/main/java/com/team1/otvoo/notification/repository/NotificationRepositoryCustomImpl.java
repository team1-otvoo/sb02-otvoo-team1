package com.team1.otvoo.notification.repository;

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
  public List<Notification> findNotificationsWithCursor(UUID receiverId, Instant cursor, UUID idAfter, int limit) {
    QNotification notification = QNotification.notification;

    return queryFactory
        .selectFrom(notification)
        .join(notification.receiver).on().fetchJoin()
        .where(
            notification.id.eq(receiverId),
            cursorCondition(notification, cursor, idAfter) // 커서 조건
        )
        .orderBy(notification.createdAt.desc(), notification.id.desc())
        .limit(limit)
        .fetch();
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

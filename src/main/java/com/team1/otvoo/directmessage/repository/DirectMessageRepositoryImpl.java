package com.team1.otvoo.directmessage.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team1.otvoo.directmessage.entity.DirectMessage;
import com.team1.otvoo.directmessage.entity.QDirectMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DirectMessageRepositoryImpl implements DirectMessageRepositoryCustom {

  private final JPAQueryFactory queryFactory;


  @Override
  public List<DirectMessage> findDirectMessagesWithCursor(UUID userId, Instant cursor, UUID idAfter, int limit) {
    QDirectMessage dm = QDirectMessage.directMessage;

    return queryFactory
        .selectFrom(dm)
        .where(
            dm.sender.id.eq(userId).or(dm.receiver.id.eq(userId)),
            cursor != null
                ? dm.createdAt.lt(cursor)
                .or(dm.createdAt.eq(cursor).and(dm.id.lt(idAfter)))
                : null
        )
        .orderBy(dm.createdAt.desc(), dm.id.desc())
        .limit(limit)
        .fetch();
  }

  @Override
  public long countDirectMessagesByUserId(UUID userId) {
    QDirectMessage dm = QDirectMessage.directMessage;

    Long count = queryFactory
        .select(dm.count())
        .from(dm)
        .where(
            dm.sender.id.eq(userId).or(dm.receiver.id.eq(userId))
        )
        .fetchOne();

    return Optional.ofNullable(count).orElse(0L);
  }
}


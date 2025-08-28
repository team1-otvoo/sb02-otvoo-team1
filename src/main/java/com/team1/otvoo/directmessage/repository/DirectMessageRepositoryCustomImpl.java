package com.team1.otvoo.directmessage.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team1.otvoo.directmessage.dto.DirectMessageDto;
import com.team1.otvoo.user.dto.UserSummary;
import com.team1.otvoo.directmessage.entity.QDirectMessage;
import com.team1.otvoo.user.entity.QUser;
import com.team1.otvoo.user.entity.QProfile;
import com.team1.otvoo.user.entity.QProfileImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DirectMessageRepositoryCustomImpl implements DirectMessageRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<DirectMessageDto> findDirectMessagesBetweenUsersWithCursor(UUID userId1, UUID userId2, Instant cursor, UUID idAfter, int limit) {
    QDirectMessage dm = QDirectMessage.directMessage;

    QUser sender = QUser.user;
    QProfile senderProfile = QProfile.profile;
    QProfileImage senderProfileImage = QProfileImage.profileImage;

    QUser receiver = new QUser("receiver");
    QProfile receiverProfile = new QProfile("receiverProfile");
    QProfileImage receiverProfileImage = new QProfileImage("receiverProfileImage");

    BooleanBuilder where = new BooleanBuilder();

    where.and(
        (dm.sender.id.eq(userId1).and(dm.receiver.id.eq(userId2)))
            .or(dm.sender.id.eq(userId2).and(dm.receiver.id.eq(userId1)))
    );

    if (cursor != null && idAfter != null) {
      where.and(
          dm.createdAt.lt(cursor)
              .or(dm.createdAt.eq(cursor).and(dm.id.lt(idAfter)))
      );
    } else if (cursor != null) {
      where.and(dm.createdAt.lt(cursor));
    }

    return queryFactory
        .select(Projections.constructor(
            DirectMessageDto.class,
            dm.id,
            dm.createdAt,
            Projections.constructor(
                UserSummary.class,
                sender.id,
                senderProfile.name,
                senderProfileImage.objectKey
            ),
            Projections.constructor(
                UserSummary.class,
                receiver.id,
                receiverProfile.name,
                receiverProfileImage.objectKey
            ),
            dm.content
        ))
        .from(dm)
        .join(dm.sender, sender)
        .leftJoin(senderProfile).on(senderProfile.user.id.eq(sender.id))
        .leftJoin(senderProfileImage).on(senderProfileImage.profile.id.eq(senderProfile.id))
        .join(dm.receiver, receiver)
        .leftJoin(receiverProfile).on(receiverProfile.user.id.eq(receiver.id))
        .leftJoin(receiverProfileImage).on(receiverProfileImage.profile.id.eq(receiverProfile.id))
        .where(where)
        .orderBy(dm.createdAt.desc(), dm.id.desc())
        .limit(limit)
        .fetch();
  }

  @Override
  public long countDirectMessagesBetweenUsers(UUID userId1, UUID userId2) {
    QDirectMessage dm = QDirectMessage.directMessage;

    Long count = queryFactory
        .select(dm.count())
        .from(dm)
        .where(
            (dm.sender.id.eq(userId1).and(dm.receiver.id.eq(userId2)))
                .or(dm.sender.id.eq(userId2).and(dm.receiver.id.eq(userId1)))
        )
        .fetchOne();

    return Optional.ofNullable(count).orElse(0L);
  }

  @Override
  public DirectMessageDto findByIdWithUserSummaries(UUID id) {
    QDirectMessage dm = QDirectMessage.directMessage;
    QUser sender = QUser.user;
    QProfile senderProfile = QProfile.profile;
    QProfileImage senderProfileImage = QProfileImage.profileImage;
    QUser receiver = new QUser("receiver");
    QProfile receiverProfile = new QProfile("receiverProfile");
    QProfileImage receiverProfileImage = new QProfileImage("receiverProfileImage");

    return queryFactory
        .select(Projections.constructor(
            DirectMessageDto.class,
            dm.id,
            dm.createdAt,
            Projections.constructor(
                UserSummary.class,
                sender.id,
                senderProfile.name,
                senderProfileImage.objectKey
            ),
            Projections.constructor(
                UserSummary.class,
                receiver.id,
                receiverProfile.name,
                receiverProfileImage.objectKey
            ),
            dm.content
        ))
        .from(dm)
        .join(dm.sender, sender)
        .leftJoin(senderProfile).on(senderProfile.user.id.eq(sender.id))
        .leftJoin(senderProfileImage).on(senderProfileImage.profile.id.eq(senderProfile.id))
        .join(dm.receiver, receiver)
        .leftJoin(receiverProfile).on(receiverProfile.user.id.eq(receiver.id))
        .leftJoin(receiverProfileImage).on(receiverProfileImage.profile.id.eq(receiverProfile.id))
        .where(dm.id.eq(id))
        .fetchOne();
  }
}
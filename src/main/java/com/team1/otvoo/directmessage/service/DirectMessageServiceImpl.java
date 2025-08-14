package com.team1.otvoo.directmessage.service;

import com.team1.otvoo.directmessage.dto.DirectMessageCreateRequest;
import com.team1.otvoo.directmessage.dto.DirectMessageDto;
import com.team1.otvoo.directmessage.dto.DirectMessageDtoCursorResponse;
import com.team1.otvoo.directmessage.entity.DirectMessage;
import com.team1.otvoo.directmessage.event.DirectMessageEvent;
import com.team1.otvoo.directmessage.repository.DirectMessageRepositoryCustom;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.UserRepository;
import com.team1.otvoo.directmessage.repository.DirectMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectMessageServiceImpl implements DirectMessageService {

  private final UserRepository userRepository;
  private final DirectMessageRepository directMessageRepository;
  private final DirectMessageRepositoryCustom directMessageRepositoryCustom;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional
  public DirectMessageDto createDto(DirectMessageCreateRequest request) {
    log.info("✅ DM 생성 요청: senderId={}, receiverId={}", request.senderId(), request.receiverId());

    User sender = userRepository.findById(request.senderId())
        .orElseThrow(() -> {
          log.error("❌ 보낸 사용자 없음: userId={}", request.senderId());
          return new RestException(ErrorCode.NOT_FOUND,
              Map.of("userId", request.senderId(), "message", "보내는 사용자를 찾을 수 없습니다"));
        });

    User receiver = userRepository.findById(request.receiverId())
        .orElseThrow(() -> {
          log.error("❌ 받는 사용자 없음: userId={}", request.receiverId());
          return new RestException(ErrorCode.NOT_FOUND,
              Map.of("userId", request.receiverId(), "message", "받는 사용자를 찾을 수 없습니다"));
        });

    DirectMessage directMessage = DirectMessage.builder()
        .sender(sender)
        .receiver(receiver)
        .content(request.content())
        .createdAt(Instant.now())
        .build();

    DirectMessage savedDirectMessage = directMessageRepository.save(directMessage);

    log.info("✅ DM 저장 완료: DM ID={}, senderId={}, receiverId={}",
        directMessage.getId(), sender.getId(), receiver.getId());

    eventPublisher.publishEvent(new DirectMessageEvent(savedDirectMessage));

    return directMessageRepositoryCustom.findByIdWithUserSummaries(directMessage.getId());
  }

  @Override
  @Transactional(readOnly = true)
  public DirectMessageDtoCursorResponse getDirectMessagesBetweenUsers(UUID userId1, UUID userId2, String cursorStr, String idAfterStr, int limit) {
    log.info("✅ DM 목록 조회 요청: userId1={}, userId2={}, cursor={}, idAfter={}, limit={}",
        userId1, userId2, cursorStr, idAfterStr, limit);

    userRepository.findById(userId1)
        .orElseThrow(() -> new RestException(ErrorCode.NOT_FOUND,
            Map.of("userId", userId1, "message", "사용자를 찾을 수 없습니다")));
    userRepository.findById(userId2)
        .orElseThrow(() -> new RestException(ErrorCode.NOT_FOUND,
            Map.of("userId", userId2, "message", "사용자를 찾을 수 없습니다")));

    Instant cursor = null;
    if (cursorStr != null && !cursorStr.isEmpty()) {
      try {
        cursor = Instant.parse(cursorStr);
      } catch (Exception e) {
        log.error("❌ cursor 파싱 실패: {}", cursorStr);
        throw new RestException(ErrorCode.INVALID_INPUT_VALUE,
            Map.of("cursor", cursorStr, "message", "cursor 형식이 올바르지 않습니다"));
      }
    }

    UUID idAfter = null;
    if (idAfterStr != null && !idAfterStr.isEmpty()) {
      try {
        idAfter = UUID.fromString(idAfterStr);
      } catch (Exception e) {
        log.error("❌ idAfter 파싱 실패: {}", idAfterStr);
        throw new RestException(ErrorCode.INVALID_INPUT_VALUE,
            Map.of("idAfter", idAfterStr, "message", "idAfter 형식이 올바르지 않습니다"));
      }
    }

    int pageSize = limit + 1;
    List<DirectMessageDto> messages = directMessageRepositoryCustom.findDirectMessagesBetweenUsersWithCursor(
        userId1, userId2, cursor, idAfter, pageSize);
    long totalCount = directMessageRepositoryCustom.countDirectMessagesBetweenUsers(userId1, userId2);

    boolean hasNext = messages.size() > limit;
    if (hasNext) {
      messages = messages.subList(0, limit);
    }

    String nextCursor = null;
    UUID nextIdAfter = null;
    if (hasNext) {
      DirectMessageDto last = messages.get(messages.size() - 1);
      nextCursor = last.createdAt().toString();
      nextIdAfter = last.id();
    }

    log.info("✅ DM 목록 조회 완료: 메시지 수={}, hasNext={}, nextCursor={}, nextIdAfter={}",
        messages.size(), hasNext, nextCursor, nextIdAfter);

    return new DirectMessageDtoCursorResponse(
        messages,
        nextCursor,
        nextIdAfter,
        hasNext,
        (int) totalCount,
        "createdAt",
        "DESCENDING"
    );
  }
}
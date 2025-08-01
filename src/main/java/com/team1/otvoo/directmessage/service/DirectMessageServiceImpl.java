package com.team1.otvoo.directmessage.service;

import com.team1.otvoo.directmessage.dto.DirectMessageCreateRequest;
import com.team1.otvoo.directmessage.dto.DirectMessageDto;
import com.team1.otvoo.directmessage.dto.DirectMessageDtoCursorResponse;
import com.team1.otvoo.directmessage.dto.DirectMessageResponse;
import com.team1.otvoo.directmessage.entity.DirectMessage;
import com.team1.otvoo.directmessage.mapper.DirectMessageMapper;
import com.team1.otvoo.directmessage.repository.DirectMessageRepositoryCustom;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.UserRepository;
import com.team1.otvoo.directmessage.repository.DirectMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
  private final DirectMessageMapper directMessageMapper;

  @Override
  @Transactional
  public DirectMessageResponse create(DirectMessageCreateRequest request) {
    log.info("✅ DM 생성 요청: senderId={}, receiverId={}", request.senderId(), request.receiverId());

    User sender = userRepository.findById(request.senderId())
        .orElseThrow(() -> {
          log.error("❌ 보내는 사용자 없음: userId={}", request.senderId());
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

    directMessageRepository.save(directMessage);

    log.info("✅ DM 저장 완료: DM ID={}, senderId={}, receiverId={}",
        directMessage.getId(), sender.getId(), receiver.getId());

    return directMessageMapper.toResponse(directMessage);
  }

  @Override
  @Transactional(readOnly = true)
  public DirectMessageDtoCursorResponse getDirectMessageByuserId(UUID userId, String cursorStr, String idAfterStr, int limit) {
    log.info("✅ DM 목록 조회 요청: userId={}, cursor={}, idAfter={}, limit={}", userId, cursorStr, idAfterStr, limit);

    userRepository.findById(userId)
        .orElseThrow(() -> new RestException(ErrorCode.NOT_FOUND,
            Map.of("userId", userId, "message", "사용자를 찾을 수 없습니다")));

    Instant cursor = null;
    if (cursorStr != null && !cursorStr.isEmpty()) {
      cursor = Instant.parse(cursorStr);
    }

    UUID idAfter = null;
    if (idAfterStr != null && !idAfterStr.isEmpty()) {
      idAfter = UUID.fromString(idAfterStr);
    }

    int pageSize = limit + 1;
    List<DirectMessage> messages = directMessageRepositoryCustom.findDirectMessagesWithCursor(userId, cursor, idAfter, pageSize);
    long totalCount = directMessageRepositoryCustom.countDirectMessagesByUserId(userId);

    boolean hasNext = messages.size() > limit;

    if (hasNext) {
      messages = messages.subList(0, limit);
    }

    List<DirectMessageDto> dtoList = messages.stream()
        .map(directMessageMapper::toDto)
        .toList();

    String nextCursor = null;
    UUID nextIdAfter = null;

    if (hasNext) {
      DirectMessage last = messages.get(messages.size() - 1);
      nextCursor = last.getCreatedAt().toString();
      nextIdAfter = last.getId();
    }

    log.info("✅ DM 목록 조회 완료: 조회된 메시지 수={}, hasNext={}, nextCursor={}, nextIdAfter={}", dtoList.size(), hasNext, nextCursor, nextIdAfter);

    return new DirectMessageDtoCursorResponse(
        dtoList,
        nextCursor,
        nextIdAfter,
        hasNext,
        (int) totalCount,
        "createdAt",
        "DESCENDING"
    );
  }
}
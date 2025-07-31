package com.team1.otvoo.directmessage.service;

import com.team1.otvoo.directmessage.dto.DirectMessageCreateRequest;
import com.team1.otvoo.directmessage.dto.DirectMessageResponse;
import com.team1.otvoo.directmessage.entity.DirectMessage;
import com.team1.otvoo.directmessage.mapper.DirectMessageMapper;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.UserRepository;
import com.team1.otvoo.directmessage.repository.DirectMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DirectMessageServiceImpl implements DirectMessageService {

  private final UserRepository userRepository;
  private final DirectMessageRepository directMessageRepository;
  private final DirectMessageMapper directMessageMapper;

  @Override
  @Transactional
  public DirectMessageResponse create(DirectMessageCreateRequest request) {
    User sender = userRepository.findById(request.senderId())
        .orElseThrow(() -> new RestException(ErrorCode.NOT_FOUND,
            Map.of("userId", request.senderId(), "message", "보내는 사용자를 찾을 수 없습니다")));

    User receiver = userRepository.findById(request.receiverId())
        .orElseThrow(() -> new RestException(ErrorCode.NOT_FOUND,
            Map.of("userId", request.receiverId(), "message", "받는 사용자를 찾을 수 없습니다")));

    DirectMessage directMessage = DirectMessage.builder()
        .sender(sender)
        .receiver(receiver)
        .content(request.content())
        .createdAt(Instant.now())
        .build();

    directMessageRepository.save(directMessage);

    return directMessageMapper.toResponse(directMessage);
  }
}

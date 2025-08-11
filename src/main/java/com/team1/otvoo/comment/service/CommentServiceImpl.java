package com.team1.otvoo.comment.service;

import com.team1.otvoo.comment.dto.CommentCreateRequest;
import com.team1.otvoo.comment.dto.CommentCursor;
import com.team1.otvoo.comment.dto.CommentDto;
import com.team1.otvoo.comment.entity.FeedComment;
import com.team1.otvoo.comment.mapper.CommentMapper;
import com.team1.otvoo.comment.repository.CommentRepository;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.feed.entity.Feed;
import com.team1.otvoo.feed.repository.FeedRepository;
import com.team1.otvoo.user.dto.AuthorDto;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.UserRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl implements CommentService{
  private final FeedRepository feedRepository;
  private final UserRepository userRepository;
  private final CommentRepository commentRepository;
  private final CommentMapper commentMapper;

  @Override
  @Transactional
  public CommentDto create(CommentCreateRequest request) {
    Feed feed = feedRepository.findById(request.feedId()).orElseThrow(
        () -> new RestException(ErrorCode.FEED_NOT_FOUND, Map.of("feedId", request.feedId()))
    );

    User user = userRepository.findById(request.authorId()).orElseThrow(
        () -> new RestException(ErrorCode.USER_NOT_FOUND, Map.of("userId", request.authorId()))
    );

    AuthorDto authorDto = userRepository.projectionAuthorDtoById(user.getId());

    FeedComment comment = new FeedComment(user, feed, request.content());
    commentRepository.save(comment);
    feedRepository.incrementCommentCount(feed.getId());

    return commentMapper.toDto(comment, authorDto);
  }

  @Override
  @Transactional(readOnly = true)
  public Slice<CommentDto> getCommentsWithCursor(CommentCursor cursor, UUID feedId) {
      return commentRepository.findCommentsWithCursor(cursor, feedId);
  }
}

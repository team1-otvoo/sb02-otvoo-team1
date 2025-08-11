package com.team1.otvoo.comment.service;

import com.team1.otvoo.comment.dto.CommentCreateRequest;
import com.team1.otvoo.comment.dto.CommentCursor;
import com.team1.otvoo.comment.dto.CommentDto;
import com.team1.otvoo.comment.entity.FeedComment;
import com.team1.otvoo.comment.mapper.CommentMapper;
import com.team1.otvoo.comment.repository.CommentRepository;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.feed.entity.Feed;
import com.team1.otvoo.feed.repository.FeedRepository;
import com.team1.otvoo.user.dto.AuthorDto;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.shaded.org.checkerframework.checker.units.qual.C;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {

  @Mock
  UserRepository userRepository;
  @Mock
  FeedRepository feedRepository;
  @Mock
  CommentRepository commentRepository;
  @Mock
  CommentMapper commentMapper;
  @InjectMocks
  CommentServiceImpl commentService;

  @Test
  @DisplayName("댓글 생성 성공")
  void comment_create_success() {
    // given
    User user = User.builder()
        .email("test@test.com")
        .build();
    ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

    Feed feed = Feed.builder()
        .user(user)
        .content("testFeed")
        .build();
    ReflectionTestUtils.setField(feed, "id", UUID.randomUUID());

    CommentCreateRequest request = new CommentCreateRequest(feed.getId() ,user.getId(), "commentTest");

    AuthorDto authorDto = new AuthorDto(user.getId(), "양고기", "image.url");
    given(userRepository.projectionAuthorDtoById(user.getId())).willReturn(authorDto);
    given(userRepository.findById(any(UUID.class))).willReturn(Optional.of(user));
    given(feedRepository.findById(any(UUID.class))).willReturn(Optional.of(feed));
    given(commentMapper.toDto(any(FeedComment.class), eq(authorDto))).willAnswer(invocation -> {
      FeedComment commentArg = invocation.getArgument(0);
      return new CommentDto(commentArg.getId(), commentArg.getCreatedAt(),
          commentArg.getFeed().getId(), authorDto, commentArg.getContent());
    });

    // when
    CommentDto result =  commentService.create(request);

    // then
    assertThat(result.content()).isEqualTo("commentTest");
    then(commentRepository).should(times(1)).save(any());
  }

  @Test
  @DisplayName("댓글 생성 실패 - 유저가 존재하지 않을 때")
  void comment_create_failed_when_userNotFound() {
    // given
    Feed feed = Feed.builder()
        .content("testFeed")
        .build();
    ReflectionTestUtils.setField(feed, "id", UUID.randomUUID());
    given(feedRepository.findById(any(UUID.class))).willReturn(Optional.of(feed));
    given(userRepository.findById(any(UUID.class))).willReturn(Optional.empty());
    CommentCreateRequest request = new CommentCreateRequest(feed.getId() ,UUID.randomUUID(), "commentTest");

    // when & then
    assertThatThrownBy(() -> commentService.create(request))
        .isInstanceOf(RestException.class)
        .hasMessageContaining("유저가 존재하지 않습니다.");
  }

  @Test
  @DisplayName("댓글 생성 실패 - 피드가 존재하지 않을 때")
  void comment_create_failed_when_feedNotFound() {
    // given
    User user = User.builder()
        .email("test@test.com")
        .build();
    given(feedRepository.findById(any(UUID.class))).willReturn(Optional.empty());
    CommentCreateRequest request = new CommentCreateRequest(UUID.randomUUID() ,UUID.randomUUID(), "commentTest");

    // when & then
    assertThatThrownBy(() -> commentService.create(request))
        .isInstanceOf(RestException.class)
        .hasMessageContaining("피드가 존재하지 않습니다.");
  }

  @Test
  @DisplayName("댓글 목록 조회 성공")
  void comment_find_with_cursor_success() {
    // given
    UUID feedId = UUID.randomUUID();
    CommentDto commentDto = new CommentDto(
        UUID.randomUUID(),
        Instant.now(),
        feedId,
        new AuthorDto(UUID.randomUUID(), "test", "test.url"),
        "testComment");
    CommentCursor commentCursor = mock(CommentCursor.class);

    Slice<CommentDto> commentDtoSlice = new SliceImpl<>(List.of(commentDto));
    given(commentRepository.findCommentsWithCursor(any(), any())).willReturn(commentDtoSlice);

    // when
    Slice<CommentDto> results = commentService.getCommentsWithCursor(commentCursor, feedId);

    // then
    assertThat(results.getContent().size()).isEqualTo(1);
    assertThat(results.getContent().get(0).content()).isEqualTo("testComment");
  }
}
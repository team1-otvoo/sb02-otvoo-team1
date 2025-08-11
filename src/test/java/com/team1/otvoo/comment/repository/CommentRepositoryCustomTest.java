package com.team1.otvoo.comment.repository;

import com.team1.otvoo.comment.dto.CommentCursor;
import com.team1.otvoo.comment.dto.CommentDto;
import com.team1.otvoo.comment.entity.FeedComment;
import com.team1.otvoo.common.AbstractPostgresTest;
import com.team1.otvoo.config.QueryDslConfig;
import com.team1.otvoo.feed.entity.Feed;
import com.team1.otvoo.user.entity.Profile;
import com.team1.otvoo.user.entity.ProfileImage;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Slice;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QueryDslConfig.class)
class CommentRepositoryCustomTest extends AbstractPostgresTest {

  @Autowired
  private CommentRepository commentRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private CommentRepositoryCustomImpl commentRepositoryCustom;

  private Feed feed;
  private User author;

  @BeforeEach
  void setUp() {
    // 작성자 생성
    author = User.builder()
        .email("author@test.com")
        .password("password")
        .build();
    userRepository.save(author);

    Profile profile = new Profile("작성자", author);

    ProfileImage profileImage = new ProfileImage("https://image.com/profile.png", "file", "jpeg",3L,3,3,profile);

    entityManager.persist(profile);
    entityManager.persist(profileImage);

    // 피드 생성
    feed = Feed.builder()
        .content("테스트 피드")
        .user(author)
        .build();
    entityManager.persist(feed);

    // 댓글 5개 생성 (createdAt 오름차순)
    for (int i = 1; i <= 5; i++) {
      FeedComment comment = new FeedComment(author, feed, "댓글 " + i);
      ReflectionTestUtils.setField(comment, "createdAt", Instant.parse("2024-12-0" + i + "T10:00:00Z"));
      entityManager.persist(comment);
    }

    entityManager.flush();
    entityManager.clear();
  }

  @Test
  @DisplayName("커서 없이 댓글 조회 - 기본 limit 반환")
  void findCommentsWithCursor_noCursor() {
    CommentCursor cursor = new CommentCursor(null, null, 3);
    Slice<CommentDto> result = commentRepositoryCustom.findCommentsWithCursor(cursor, feed.getId());

    assertThat(result.getContent()).hasSize(3);
    assertThat(result.getContent().get(0).content()).isEqualTo("댓글 1");
    assertThat(result.hasNext()).isTrue();
  }

  @Test
  @DisplayName("커서 기반 다음 페이지 조회")
  void findCommentsWithCursor_withCursor() {
    // 첫 페이지 조회
    CommentCursor first = new CommentCursor(null, null, 3);
    Slice<CommentDto> firstResult = commentRepositoryCustom.findCommentsWithCursor(first, feed.getId());

    // 커서값 생성: 마지막 댓글의 createdAt
    Instant lastCreatedAt = firstResult.getContent().get(2).createdAt();
    UUID lastId = firstResult.getContent().get(2).id();

    CommentCursor nextCursor = new CommentCursor(lastCreatedAt.toString(), lastId, 3);
    Slice<CommentDto> secondResult = commentRepositoryCustom.findCommentsWithCursor(nextCursor, feed.getId());

    assertThat(secondResult.getContent()).hasSize(2);
    assertThat(secondResult.getContent().get(0).content()).isEqualTo("댓글 4");
    assertThat(secondResult.hasNext()).isFalse();
  }

  @Test
  @DisplayName("댓글이 없는 경우 빈 Slice 반환")
  void findCommentsWithCursor_emptyResult() {
    UUID otherFeedId = UUID.randomUUID();
    CommentCursor cursor = new CommentCursor(null, null, 3);
    Slice<CommentDto> result = commentRepositoryCustom.findCommentsWithCursor(cursor, otherFeedId);

    assertThat(result).isEmpty();
    assertThat(result.hasNext()).isFalse();
  }
}
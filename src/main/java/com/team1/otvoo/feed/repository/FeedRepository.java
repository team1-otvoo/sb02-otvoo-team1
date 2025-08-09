package com.team1.otvoo.feed.repository;

import com.team1.otvoo.feed.entity.Feed;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedRepository extends JpaRepository<Feed, UUID>, FeedRepositoryCustom {
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE Feed f SET f.likeCount = f.likeCount + 1 WHERE f.id = :id")
  void incrementLikeCount(@Param("id") UUID id);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE Feed f SET f.likeCount = f.likeCount - 1 WHERE f.id = :id")
  void decrementLikerCount(@Param("id") UUID id);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE Feed f SET f.commentCount = f.commentCount + 1 WHERE f.id = :id")
  void incrementCommentCount(@Param("id") UUID id);
}

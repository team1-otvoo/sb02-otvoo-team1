package com.team1.otvoo.feed.repository;

import com.team1.otvoo.feed.entity.FeedLike;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedLikeRepository extends JpaRepository<FeedLike, UUID> {
  boolean existsFeedLikeByFeed_IdAndLikedBy_Id(UUID feedId, UUID likedById);
  void deleteByFeed_IdAndLikedBy_Id(UUID feedId, UUID likedById);
}

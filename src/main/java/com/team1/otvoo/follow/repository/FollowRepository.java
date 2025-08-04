package com.team1.otvoo.follow.repository;

import com.team1.otvoo.follow.entity.Follow;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowRepository extends JpaRepository<Follow, UUID>, FollowRepositoryCustom {
  boolean existsByFolloweeIdAndFollowerId(UUID followeeId, UUID followerId);

  @Query("SELECT COUNT(f) FROM Follow f WHERE f.follower.id = :followerId")
  long countByFollowerId(@Param("followerId") UUID followerId);

  @Query("SELECT COUNT(f) FROM Follow f WHERE f.followee.id = :followeeId")
  long countByFolloweeId(@Param("followeeId") UUID followeeId);
}

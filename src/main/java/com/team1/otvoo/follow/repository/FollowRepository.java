package com.team1.otvoo.follow.repository;

import com.team1.otvoo.follow.entity.Follow;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowRepository extends JpaRepository<Follow, UUID> {
  boolean existsByFolloweeIdAndFollowerId(UUID followeeId, UUID followerId);
}

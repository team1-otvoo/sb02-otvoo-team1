package com.team1.otvoo.follow.repository;

import com.team1.otvoo.follow.entity.Follow;
import com.team1.otvoo.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowRepository extends JpaRepository<Follow, UUID>, FollowRepositoryCustom {
  boolean existsByFolloweeIdAndFollowerId(UUID followeeId, UUID followerId);

  Optional<Follow> findByFolloweeIdAndFollowerId(UUID followeeId, UUID followerId);

  @Query("SELECT f.follower FROM Follow f WHERE f.followee.id = :followeeId")
  Page<User> findFollowersByFolloweeId(UUID followeeId, Pageable pageable);
}

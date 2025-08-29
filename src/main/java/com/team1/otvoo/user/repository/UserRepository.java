package com.team1.otvoo.user.repository;

import com.team1.otvoo.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, UserRepositoryCustom {

  boolean existsByEmail(String email);
  Optional<User> findByEmail(String email);

  @Modifying
  @Query("UPDATE User u SET u.followerCount = u.followerCount + 1 WHERE u.id = :id")
  void incrementFollowerCount(@Param("id") UUID userId);

  @Modifying
  @Query("UPDATE User u SET u.followingCount = u.followingCount + 1 WHERE u.id = :id")
  void incrementFollowingCount(@Param("id") UUID userId);

  @Modifying
  @Query("UPDATE User u SET u.followerCount = GREATEST(u.followerCount - 1, 0) WHERE u.id = :id")
  void decrementFollowerCount(@Param("id") UUID userId);

  @Modifying
  @Query("UPDATE User u SET u.followingCount = GREATEST(u.followingCount - 1, 0) WHERE u.id = :id")
  void decrementFollowingCount(@Param("id") UUID userId);
}

package com.team1.otvoo.user.repository;

import com.team1.otvoo.user.entity.Profile;
import com.team1.otvoo.user.projection.UserNameView;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, UUID> {
  @Query("""
    SELECT p.user.id AS userId, p.name AS name
    FROM Profile p
    WHERE p.user.id IN :userIds
""")
  List<UserNameView> findUserNamesByUserIds(@Param("userIds") List<UUID> userIds);
}

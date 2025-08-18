package com.team1.otvoo.user.repository;

import com.team1.otvoo.user.entity.ProfileImage;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProfileImageRepository extends JpaRepository<ProfileImage, UUID> {
  Optional<ProfileImage> findByProfileId(UUID profileId);

  @Query("""
    SELECT pi.imageUrl
    FROM ProfileImage pi
    WHERE pi.profile.id = :profileId
""")
  Optional<String> findUrlByProfileId(UUID profileId);
}
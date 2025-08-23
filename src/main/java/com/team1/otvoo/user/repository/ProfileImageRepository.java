package com.team1.otvoo.user.repository;

import com.team1.otvoo.user.entity.ProfileImage;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileImageRepository extends JpaRepository<ProfileImage, UUID> {
  Optional<ProfileImage> findByProfileId(UUID profileId);
}
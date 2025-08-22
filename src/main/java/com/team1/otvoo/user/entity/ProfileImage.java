package com.team1.otvoo.user.entity;

import com.team1.otvoo.user.dto.ProfileImageMetaData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "profile_images")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ProfileImage{
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "object_key", length = 255)
  private String objectKey;

  @Column(name = "original_filename", length = 255)
  private String originalFilename;

  @Column(name = "content_type", length = 50)
  private String contentType;

  @Column
  private Long size;

  @Column
  private Integer width;

  @Column
  private Integer height;

  @Column(name = "uploaded_at")
  private Instant uploadedAt;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "profile_id", unique = true)
  private Profile profile;

  public ProfileImage(
      String objectKey,
      String originalFilename,
      String contentType,
      Long size,
      Integer width,
      Integer height,
      Profile profile
  ) {
    this.objectKey = objectKey;
    this.originalFilename = originalFilename;
    this.contentType = contentType;
    this.size = size;
    this.width = width;
    this.height = height;
    this.uploadedAt = Instant.now();
    this.profile = profile;
  }

  // ProfileImage 엔티티 내부
  public void updateMetaData(ProfileImageMetaData metaData) {

    // 식별자(id)와 연관(profile)은 유지하고, 메타데이터만 교체
    this.objectKey = metaData.objectKey();
    this.originalFilename = metaData.originalFilename();
    this.contentType = metaData.contentType();
    this.size = metaData.size();
    this.width = metaData.width();
    this.height = metaData.height();
    this.uploadedAt = Instant.now();
  }
}
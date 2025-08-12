package com.team1.otvoo.user.entity;

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

  @Column(name = "image_url")
  private String imageUrl;

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
      String imageUrl,
      String originalFilename,
      String contentType,
      Long size,
      Integer width,
      Integer height,
      Profile profile
  ) {
    this.imageUrl = imageUrl;
    this.originalFilename = originalFilename;
    this.contentType = contentType;
    this.size = size;
    this.width = width;
    this.height = height;
    this.uploadedAt = Instant.now();
    this.profile = profile;
  }

  // ProfileImage 엔티티 내부
  public void updateFrom(ProfileImage src) {
    if (src == null) {
      return;
    }
    // 동일 프로필에 대한 교체만 허용 (안전장치)
    if (this.getProfile() == null || src.getProfile() == null
        || !this.getProfile().getId().equals(src.getProfile().getId())) {
      return;
    }

    // 식별자(id)와 연관(profile)은 유지하고, 메타데이터만 교체
    this.imageUrl = src.getImageUrl();
    this.originalFilename = src.getOriginalFilename();
    this.contentType = src.getContentType();
    this.size = src.getSize();
    this.width = src.getWidth();
    this.height = src.getHeight();
    this.uploadedAt = Instant.now();

    // JPA Auditing을 쓰신다면 @LastModifiedDate 로 갱신되므로 별도 처리 불필요
    // (직접 관리한다면 여기서 updatedAt = Instant.now();)
  }
}
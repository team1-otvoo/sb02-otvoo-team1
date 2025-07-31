package com.team1.otvoo.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

  @OneToOne
  @JoinColumn(name = "profile_id", unique = true)
  private Profile profile;
}
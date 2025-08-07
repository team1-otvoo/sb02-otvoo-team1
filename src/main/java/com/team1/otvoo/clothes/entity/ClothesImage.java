package com.team1.otvoo.clothes.entity;

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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "clothes_images")
@Getter
@NoArgsConstructor
public class ClothesImage {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "image_url", nullable = false)
  private String imageUrl;

  @Column(name = "file_name")
  private String fileName;

  @Column(name = "content_type")
  private String contentType;

  @Column
  private Long size;
  @Column
  private Integer width;
  @Column
  private Integer height;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "clothes_id", nullable = false, unique = true)
  private Clothes clothes;

  @Column(name = "upload_at")
  private Instant uploadAt;

  public ClothesImage(String imageUrl, String fileName, String contentType, Long size,
      Integer width, Integer height,
      Clothes clothes) {
    this.imageUrl = imageUrl;
    this.fileName = fileName;
    this.contentType = contentType;
    this.size = size;
    this.width = width;
    this.height = height;
    this.clothes = clothes;
    this.uploadAt = Instant.now();
  }
}
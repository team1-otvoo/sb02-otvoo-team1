package com.team1.otvoo.clothes.service;

import com.team1.otvoo.clothes.entity.Clothes;
import com.team1.otvoo.clothes.entity.ClothesImage;
import com.team1.otvoo.clothes.repository.ClothesImageRepository;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.storage.S3ImageStorage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClothesImageServiceImpl implements ClothesImageService {

  private static final Set<String> ALLOWED =
      Set.of("image/jpg", "image/jpeg", "image/png", "image/webp", "image/gif");
  private final ClothesImageRepository clothesImageRepository;
  private final S3ImageStorage s3ImageStorage;

  @Override
  @Transactional
  public ClothesImage create(Clothes clothes, MultipartFile file) {
    if (clothes == null) {
      throw new RestException(ErrorCode.INVALID_INPUT_VALUE, Map.of("clothes", "null"));
    }
    if (file == null || file.isEmpty()) {
      throw new RestException(ErrorCode.INVALID_INPUT_VALUE, Map.of("file", "empty"));
    }
    String contentType = normalizeContentType(file.getContentType(),
        file.getOriginalFilename());
    if (contentType == null || !ALLOWED.contains(contentType)) {
      throw new RestException(ErrorCode.UNSUPPORTED_IMAGE_FORMAT,
          Map.of("contentType", String.valueOf(contentType)));
    }

    String extension = resolveExt(file.getOriginalFilename(), contentType);
    String key = "images/clothes/" + clothes.getId() + "/" + UUID.randomUUID() + "." + extension;

    byte[] bytes;
    try {
      bytes = file.getBytes();
    } catch (IOException e) {
      throw new RestException(ErrorCode.IO_EXCEPTION, Map.of("file", "파일 읽기 실패"));
    }

    Integer width = null, height = null;
    try (InputStream in = new ByteArrayInputStream(bytes)) {
      BufferedImage img = ImageIO.read(in);
      if (img != null) {
        width = img.getWidth();
        height = img.getHeight();
      }
    } catch (IOException e) {
      log.warn("width/height 추출 실패: {}", e.getMessage(), e);
    }

    try (InputStream in = new ByteArrayInputStream(bytes)) {
      s3ImageStorage.upload(key, in, bytes.length, contentType);
    } catch (Exception e) {
      throw new RestException(ErrorCode.IMAGE_UPLOAD_FAILED, Map.of("s3", "upload failed"));
    }

    Optional<ClothesImage> existingOpt = clothesImageRepository.findByClothes_Id(clothes.getId());
    String oldKey = null;
    ClothesImage result;

    try {
      if (existingOpt.isPresent()) {
        // 교체
        ClothesImage existingImage = existingOpt.get();
        oldKey = existingImage.getImageKey();
        existingImage.replace(
            key,
            file.getOriginalFilename(),
            contentType,
            (long) bytes.length,
            width,
            height
        );
        result = existingImage;
      } else {
        ClothesImage image = new ClothesImage(
            key,
            file.getOriginalFilename(),
            contentType,
            (long) bytes.length,
            width,
            height,
            clothes
        );
        result = clothesImageRepository.save(image);
      }
    } catch (RuntimeException dbException) {
      try {
        s3ImageStorage.delete(key);
      } catch (Exception e) {
        log.warn("upload 실패로 인한 s3 삭제가 실패되었습니다. key: {}", key, e);
      }
      throw dbException;
    }
    if (oldKey != null && !oldKey.equals(key)) {
      String deleteKey = oldKey;
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              try {
                s3ImageStorage.delete(deleteKey);
              } catch (Exception e) {
                log.warn("기존 이미지 S3 삭제 실패 oldKey: {}", deleteKey, e);
              }
            }
          }
      );
    }
    return result;
  }

  @Override
  @Transactional()
  public void delete(ClothesImage clothesImage) {
    if (clothesImage == null) {
      return;
    }
    String key = clothesImage.getImageKey();
    try {
      s3ImageStorage.delete(key);
    } catch (RestException e) {
      log.warn("S3 삭제 실패: key={}, msg={}", key, e.getMessage(), e);
    }
    clothesImageRepository.delete(clothesImage);
  }

  private String trimQuery(String name) {
    if (name == null) {
      return null;
    }
    int q = name.indexOf('?');
    if (q >= 0) {
      name = name.substring(0, q);
    }
    int h = name.indexOf('#');
    if (h >= 0) {
      name = name.substring(0, h);
    }
    return name;
  }

  private String normalizeContentType(String raw, String fileName) {
    String contentType = raw == null ? null : raw.toLowerCase();
    if (contentType == null || contentType.isBlank() || contentType.equals(
        "application/octet-stream")) {
      contentType = guessFromFileName(fileName);

    }
    return contentType;
  }

  private String guessFromFileName(String fileName) {
    fileName = trimQuery(fileName);
    if (fileName == null) {
      return null;
    }
    int dot = fileName.lastIndexOf('.');
    if (dot < 0 || dot == fileName.length() - 1) {
      return null;
    }
    String ext = fileName.substring(dot + 1).toLowerCase();
    return switch (ext) {
      case "jpg", "jpeg" -> "image/jpeg";
      case "png" -> "image/png";
      case "gif" -> "image/gif";
      case "webp" -> "image/webp";
      default -> null;
    };
  }

  private String resolveExt(String fileName, String contentType) {
    fileName = trimQuery(fileName);
    String ext = null;
    if (fileName != null) {
      int dot = fileName.lastIndexOf('.');
      if (dot >= 0 && dot < fileName.length() - 1) {
        ext = fileName.substring(dot + 1);
      }
    }
    if (ext == null) {
      switch (contentType) {
        case "image/jpeg":
        case "image/jpg":
          ext = "jpg";
          break;
        case "image/png":
          ext = "png";
          break;
        case "image/gif":
          ext = "gif";
          break;
        case "image/webp":
          ext = "webp";
          break;
        default:
          ext = "bin";
      }
    }
    return ext.toLowerCase();
  }
}

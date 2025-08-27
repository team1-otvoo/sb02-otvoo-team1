package com.team1.otvoo.storage;

import com.team1.otvoo.config.props.S3Props;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3ImageStorageAdapter implements S3ImageStorage {

  private final S3Client s3Client;
  private final S3Presigner s3Presigner;
  private final S3Props props;

  @Override
  public String upload(String key, InputStream in, long length, String contentType) {
    if (key == null || key.isBlank()) {
      throw new IllegalArgumentException("key는 필수입니다.");
    }

    try {
      PutObjectRequest request = PutObjectRequest.builder()
          .bucket(props.getBucket())
          .key(key)
          .contentLength(length)
          .contentType(contentType)
          .build();
      s3Client.putObject(request, RequestBody.fromInputStream(in, length));
      log.info("S3 업로드 성공:{}/{}", props.getBucket(), key);

      return key;
    } catch (S3Exception e) {
      log.error("S3 업로드 실패: bucket={}, key={}, code={}, msg={}",
          props.getBucket(), key,
          e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "N/A",
          e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage(), e);
      throw new RestException(ErrorCode.IO_EXCEPTION, Map.of("message", "S3 업로드 실패: " + key));
    }
  }

  @Override
  public void delete(String key) {
    try {
      var req = DeleteObjectRequest.builder()
          .bucket(props.getBucket())
          .key(key)
          .build();

      s3Client.deleteObject(req);

      // 객체가 없어도 S3는 보통 204로 성공 처리합니다.
      log.info("S3 삭제 요청: bucket={}, key={}", props.getBucket(), key);

    } catch (S3Exception e) {
      // 권한/버킷오류 등 서비스 측 예외
      log.error("S3 삭제 실패: bucket={}, key={}, code={}, msg={}",
          props.getBucket(), key,
          e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "N/A",
          e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage(),
          e);
      throw new RestException(ErrorCode.IO_EXCEPTION, Map.of("message", "S3 객체 삭제 실패: " + key));
    } catch (Exception e) {
      // 네트워크 등 클라이언트 측 예외
      log.error("S3 삭제 중 에러: bucket={}, key={}, msg={}",
          props.getBucket(), key, e.getMessage(), e);
      throw new RestException(ErrorCode.INTERNAL_SERVER_ERROR,
          Map.of("message", "S3 객체 삭제 중 알 수 없는 오류: " + key));
    }
  }

  @Override
  public String getPresignedUrl(String key) {
    try {
      long ttlSec = props.getPresignedExpirationSeconds();

      GetObjectRequest.Builder getReqBuilder = GetObjectRequest.builder()
          .bucket(props.getBucket())
          .key(key);

      GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
          .signatureDuration(Duration.ofSeconds(ttlSec))
          .getObjectRequest(getReqBuilder.build())
          .build();

      PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignReq);
      String url = presigned.url().toString();

      log.info("Presigned GET URL 생성: bucket={}, key={}, ttlSec={}",
          props.getBucket(), key, ttlSec);

      return url;

    } catch (S3Exception e) {
      log.error("Presigned URL 생성 실패: bucket={}, key={}, code={}, msg={}",
          props.getBucket(), key,
          e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "N/A",
          e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage(), e);

      throw new RestException(
          ErrorCode.IO_EXCEPTION,
          Map.of("message", "S3 Presigned URL 생성 실패: " + key)
      );

    } catch (Exception e) {
      log.error("Presigned URL 생성 중 알 수 없는 오류: bucket={}, key={}, msg={}",
          props.getBucket(), key, e.getMessage(), e);

      throw new RestException(
          ErrorCode.INTERNAL_SERVER_ERROR,
          Map.of("message", "S3 Presigned URL 생성 중 알 수 없는 오류: " + key)
      );
    }
  }

  @Override
  public String getPresignedUrl(String key, String contentType) {
    try {
      long ttlSec = props.getPresignedExpirationSeconds();

      GetObjectRequest.Builder getReqBuilder = GetObjectRequest.builder()
          .bucket(props.getBucket())
          .key(key);

      if (contentType != null && !contentType.isBlank()) {
        getReqBuilder.responseContentType(contentType);
      }

      GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
          .signatureDuration(Duration.ofSeconds(ttlSec))
          .getObjectRequest(getReqBuilder.build())
          .build();

      PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignReq);
      String url = presigned.url().toString();

      log.info("Presigned GET URL 생성: bucket={}, key={}, ttlSec={}",
          props.getBucket(), key, ttlSec);

      return url;

    } catch (S3Exception e) {
      log.error("Presigned URL 생성 실패: bucket={}, key={}, code={}, msg={}",
          props.getBucket(), key,
          e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "N/A",
          e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage(), e);

      throw new RestException(
          ErrorCode.IO_EXCEPTION,
          Map.of("message", "S3 Presigned URL 생성 실패: " + key)
      );

    } catch (Exception e) {
      log.error("Presigned URL 생성 중 알 수 없는 오류: bucket={}, key={}, msg={}",
          props.getBucket(), key, e.getMessage(), e);

      throw new RestException(
          ErrorCode.INTERNAL_SERVER_ERROR,
          Map.of("message", "S3 Presigned URL 생성 중 알 수 없는 오류: " + key)
      );
    }
  }

  @Override
  public byte[] download(String key) throws IOException {
    if (key == null || key.isBlank()) {
      throw new IllegalArgumentException("key는 필수입니다.");
    }

    try {
      GetObjectRequest request = GetObjectRequest.builder()
          .bucket(props.getBucket())
          .key(key)
          .build();

      // getObjectAsBytes()를 사용하면 SDK가 스트림을 읽고 닫는 것을 모두 처리해줍니다.
      ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(request);

      log.info("S3 다운로드 성공 (bytes): {}/{}", props.getBucket(), key);
      return responseBytes.asByteArray();

    } catch (S3Exception e) {
      log.error("S3 다운로드 실패: bucket={}, key={}, code={}, msg={}",
          props.getBucket(), key,
          e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "N/A",
          e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage(), e);
      throw new RestException(ErrorCode.NOT_FOUND, Map.of("message", "S3 파일을 찾을 수 없습니다: " + key));
    }
  }
}
package com.team1.otvoo.storage;

import com.team1.otvoo.config.props.S3Props;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import java.io.IOException;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("S3ImageStorageAdapter 단위 테스트")
@ExtendWith(MockitoExtension.class)
class S3ImageStorageAdapterTest {

  @InjectMocks
  private S3ImageStorageAdapter s3ImageStorageAdapter;

  @Mock
  private S3Client s3Client;
  @Mock
  private S3Presigner s3Presigner;
  @Mock
  private S3Props s3Props;

  private static final String BUCKET_NAME = "test-bucket";
  private static final String KEY = "path/to/image.jpg";
  private static final String CONTENT_TYPE = "image/jpeg";

  @Nested
  @DisplayName("upload 메소드는")
  class UploadTest {

    private InputStream inputStream;
    private byte[] content;

    @BeforeEach
    void setUp() {
      // 이 부분은 모든 테스트에서 공통으로 사용되므로 유지합니다.
      content = "test data".getBytes();
      inputStream = new ByteArrayInputStream(content);
    }

    @Test
    // ✅ 테스트 이름도 의도에 맞게 수정하면 더 좋습니다.
    @DisplayName("이미지 업로드 성공 시 객체 key를 반환한다")
    void should_returnObjectKey_when_uploadSuccess() { // throws MalformedURLException 제거
      // given
      given(s3Props.getBucket()).willReturn(BUCKET_NAME);

      // when
      String actualKey = s3ImageStorageAdapter.upload(KEY, inputStream, content.length, CONTENT_TYPE);

      // then
      assertThat(actualKey).isEqualTo(KEY);

      verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));

    }

    @Test
    @DisplayName("key가 null이면 IllegalArgumentException을 던진다")
    void should_throwIllegalArgumentException_when_keyIsNull() {
      // given
      // 이 테스트는 s3Props.getBucket()을 호출하지 않으므로 stubbing이 필요 없습니다.
      InputStream emptyStream = new ByteArrayInputStream(new byte[0]);

      // when & then
      assertThrows(IllegalArgumentException.class,
          () -> s3ImageStorageAdapter.upload(null, emptyStream, 0, CONTENT_TYPE));
    }

    @Test
    @DisplayName("S3 작업 중 예외 발생 시 RestException을 던진다")
    void should_throwRestException_when_s3ExceptionOccurs() {
      // given
      // 1. UnnecessaryStubbingException 해결: 이 테스트에서도 bucket 이름이 필요
      given(s3Props.getBucket()).willReturn(BUCKET_NAME);

      given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
          .willThrow(S3Exception.builder().message("S3 access denied").build());

      // when & then
      RestException exception = assertThrows(RestException.class,
          () -> s3ImageStorageAdapter.upload(KEY, inputStream, content.length, CONTENT_TYPE)); // contentLength 수정

      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.IO_EXCEPTION);
    }
  }

  @Nested
  @DisplayName("delete 메소드는")
  class DeleteTest {

    @Test
    @DisplayName("성공적으로 S3 객체 삭제를 요청한다")
    void should_requestDeletion_when_deleteSuccess() {
      // given (void 메소드이므로 별도의 given 동작 불필요)

      // when
      s3ImageStorageAdapter.delete(KEY);

      // then
      verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("S3 작업 중 예외 발생 시 RestException을 던진다")
    void should_throwRestException_when_s3ExceptionOccurs() {
      // given
      given(s3Client.deleteObject(any(DeleteObjectRequest.class)))
          .willThrow(S3Exception.builder().message("S3 error").build());

      // when & then
      RestException exception = assertThrows(RestException.class, () -> s3ImageStorageAdapter.delete(KEY));
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.IO_EXCEPTION);
    }
  }

  @Nested
  @DisplayName("getPresignedUrl 메소드는")
  class GetPresignedUrlTest {

    @BeforeEach
    void setUp() {
      // given: 모든 getPresignedUrl 테스트에서 공통적으로 필요한 만료 시간 설정
      long expirationSec = 3600L;
      given(s3Props.getPresignedExpirationSeconds()).willReturn(expirationSec);
    }

    @Test
    @DisplayName("객체 조회를 위한 Presigned URL을 성공적으로 생성한다")
    void should_createPresignedUrl_when_requestIsValid() throws MalformedURLException {
      // given
      String expectedUrl = String.format("https://%s.s3.ap-northeast-2.amazonaws.com/%s?presigned-signature=...", BUCKET_NAME, KEY);
      PresignedGetObjectRequest mockPresignedRequest = mock(PresignedGetObjectRequest.class);

      given(mockPresignedRequest.url()).willReturn(new URL(expectedUrl));
      given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
          .willReturn(mockPresignedRequest);

      // when
      String actualUrl = s3ImageStorageAdapter.getPresignedUrl(KEY, CONTENT_TYPE);

      // then
      assertThat(actualUrl).isEqualTo(expectedUrl);
      verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    @DisplayName("S3 Presigner 작업 중 예외 발생 시 RestException을 던진다")
    void should_throwRestException_when_s3ExceptionOccurs() {
      // given
      given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
          .willThrow(S3Exception.builder().message("Presigner error").build());

      // when & then
      RestException exception = assertThrows(RestException.class,
          () -> s3ImageStorageAdapter.getPresignedUrl(KEY, CONTENT_TYPE));
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.IO_EXCEPTION);
    }
  }

  @Nested
  @DisplayName("download 메소드는")
  class DownloadTest {

    @Test
    @DisplayName("S3 객체를 byte 배열로 성공적으로 다운로드한다")
    void should_returnByteArray_when_downloadSuccess() throws IOException {
      // given
      byte[] expectedBytes = "file content".getBytes();
      ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), expectedBytes);

      given(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).willReturn(responseBytes);

      // when
      byte[] actualBytes = s3ImageStorageAdapter.download(KEY);

      // then
      assertThat(actualBytes).isEqualTo(expectedBytes);
    }

    @Test
    @DisplayName("key가 공백이면 IllegalArgumentException을 던진다")
    void should_throwIllegalArgumentException_when_keyIsBlank() {
      // when & then
      assertThrows(IllegalArgumentException.class, () -> s3ImageStorageAdapter.download("  "));
    }

    @Test
    @DisplayName("S3 객체를 찾을 수 없으면 NOT_FOUND 에러코드와 함께 RestException을 던진다")
    void should_throwNotFoundRestException_when_objectNotFound() {
      // given
      given(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
          .willThrow(S3Exception.builder().statusCode(404).message("Not Found").build());

      // when & then
      RestException exception = assertThrows(RestException.class, () -> s3ImageStorageAdapter.download(KEY));
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
    }
  }
}
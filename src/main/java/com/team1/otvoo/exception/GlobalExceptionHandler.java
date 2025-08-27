package com.team1.otvoo.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception e) {
    log.error("예상치 못한 서버 내부 오류 처리: {} ", e.getMessage(), e);
    ErrorResponse errorResponse = new ErrorResponse(

        ErrorCode.INTERNAL_SERVER_ERROR.toString(),
        ErrorCode.INTERNAL_SERVER_ERROR.getMessage(),
        Map.of()
    );
    return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus()).body(errorResponse);
  }

  @ExceptionHandler(RestException.class)
  public ResponseEntity<ErrorResponse> handleRestException(RestException e) {
    log.warn("RestException 예외 처리 - errorCode: {}", e.getErrorCode().name());
    return ResponseEntity.status(e.getErrorCode().getStatus()).body(new ErrorResponse(e));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
    log.warn("요청 유효성 검증 실패: {}", e.getMessage());

    Map<String, Object> errors = e.getBindingResult()
        .getFieldErrors()
        .stream()
        .collect(Collectors.toMap(
            fieldError -> fieldError.getField(),
            fieldError -> fieldError.getDefaultMessage(),
            (msg1, msg2) -> msg1 // 중복 필드는 첫 번째 메시지 사용
        ));

    ErrorResponse errorResponse = new ErrorResponse(
        ErrorCode.VALIDATION_ERROR.toString(),
        ErrorCode.VALIDATION_ERROR.getMessage(),
        errors
    );
    return ResponseEntity.badRequest().body(errorResponse);
  }

  // tomcat , DispatcherServlet 에서 잡는 예외
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ErrorResponse> handleMaxUpload(MaxUploadSizeExceededException e) {
    log.warn("파일 업로드 용량 초과: {}", e.getMessage());
    ErrorResponse body = new ErrorResponse(
        ErrorCode.TOO_BIG_IMAGE.toString(),
        ErrorCode.TOO_BIG_IMAGE.getMessage(),
        Map.of()
    );
    return ResponseEntity.status(ErrorCode.TOO_BIG_IMAGE.getStatus()).body(body); // 413
  }

  @ExceptionHandler(MultipartException.class)
  public ResponseEntity<ErrorResponse> handleMultipart(MultipartException e) {
    log.warn("Multipart 예외: {}", e.getMessage(), e);

    if (e.getCause() instanceof MaxUploadSizeExceededException cause) {
      return handleMaxUpload(cause);
    }

    ErrorResponse body = new ErrorResponse(
        ErrorCode.INVALID_MULTIPART_REQUEST.toString(),
        ErrorCode.INVALID_MULTIPART_REQUEST.getMessage(),
        Map.of("reason", e.getClass().getSimpleName())
    );
    return ResponseEntity.status(ErrorCode.INVALID_MULTIPART_REQUEST.getStatus()).body(body);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
    log.warn("요청 파라미터 제약조건 위반: {}", e.getMessage());

    Map<String, Object> errors = e.getConstraintViolations()
        .stream()
        .collect(Collectors.toMap(
            v -> v.getPropertyPath().toString(),
            ConstraintViolation::getMessage,
            (m1, m2) -> m1
        ));

    ErrorResponse body = new ErrorResponse(
        ErrorCode.VALIDATION_ERROR.toString(),
        ErrorCode.VALIDATION_ERROR.getMessage(),
        errors
    );
    return ResponseEntity.badRequest().body(body);
  }
}

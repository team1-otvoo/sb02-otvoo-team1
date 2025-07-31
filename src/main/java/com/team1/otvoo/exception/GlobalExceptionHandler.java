package com.team1.otvoo.exception;

import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
}



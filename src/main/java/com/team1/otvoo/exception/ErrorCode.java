package com.team1.otvoo.exception;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum ErrorCode implements Code {

  // --- 400 Bad Request ---
  INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
  MISSING_REQUIRED_FIELD(HttpStatus.BAD_REQUEST, "필수 값이 누락되었습니다."),
  TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "요청 타입이 잘못되었습니다."),
  ENUM_TYPE_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않은 열거형 값입니다."),
  JSON_PARSE_ERROR(HttpStatus.BAD_REQUEST, "요청 본문을 파싱할 수 없습니다."),
  VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "입력 값 검증에 실패했습니다."),
  UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 미디어 타입입니다."),
  METHOD_ARGUMENT_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "요청 파라미터 타입이 올바르지 않습니다."),
  MISSING_PATH_VARIABLE(HttpStatus.BAD_REQUEST, "필수 경로 변수가 누락되었습니다."),
  MISSING_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "필수 요청 파라미터가 누락되었습니다."),
  SAME_AS_OLD_PASSWORD(HttpStatus.BAD_REQUEST, "기존 비밀번호와 동일한 비밀번호는 사용할 수 없습니다."),
  INVALID_SORT_BY_FIELD(HttpStatus.BAD_REQUEST, "지원하지 않는 정렬 필드입니다."),
  INVALID_SORT_DIRECTION(HttpStatus.BAD_REQUEST, "지원하지 않는 정렬 방향입니다."),
  UNSUPPORTED_IMAGE_FORMAT(HttpStatus.BAD_REQUEST,"지원하지 않는 이미지 포맷입니다."),
  FETCH_OR_PARSE_FAILED(HttpStatus.BAD_REQUEST, "외부 페이지를 가져오거나 파싱하는 데 실패했습니다."),
  MALFORMED_URL(HttpStatus.BAD_REQUEST, "올바르지 않은 Url 형식 입니다."),


  // --- 401 Unauthorized ---
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
  INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
  EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
  UNSUPPORTED_TOKEN(HttpStatus.UNAUTHORIZED, "지원하지 않는 토큰입니다."),
  AUTH_HEADER_MISSING(HttpStatus.UNAUTHORIZED, "Authorization 헤더가 누락되었습니다."),
  INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),

  // --- 403 Forbidden ---
  ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
  ROLE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "해당 역할로는 접근할 수 없습니다."),

  // --- 404 Not Found ---
  NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
  PATH_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 URL 경로가 존재하지 않습니다."),

  // --- 405 Method Not Allowed ---
  METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 HTTP 메서드입니다."),

  // --- 409 Conflict ---
  CONFLICT(HttpStatus.CONFLICT, "이미 존재하는 리소스입니다."),
  DATA_INTEGRITY_VIOLATION(HttpStatus.CONFLICT, "데이터 무결성 제약 조건에 위배됩니다."),

  // --- 415 Unsupported Media Type ---
  MEDIA_TYPE_NOT_SUPPORTED(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 콘텐츠 타입입니다."),

  // --- 500 Internal Server Error ---
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
  UNKNOWN_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "예기치 못한 오류가 발생했습니다."),
  IO_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "입출력 처리 중 오류가 발생했습니다."),
  DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "데이터베이스 처리 중 오류가 발생했습니다."),
  EXTERNAL_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "외부 API 호출 중 오류가 발생했습니다."),
  EXTERNAL_API_EMPTY_RESPONSE(HttpStatus.INTERNAL_SERVER_ERROR, "외부 API가 빈 응답을 반환했습니다."),
  LLM_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "LLM 처리 중 오류가 발생했습니다"),

  // --- Clothes 관련 ErrorCode ---
  CLOTHES_NOT_FOUND(HttpStatus.NOT_FOUND, "의상이 존재하지 않습니다."),
  CLOTHES_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "사용자의 id와 옷 소유자의 id가 일치하지 않습니다"),
  CLOTHES_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "의상 이미지가 존재하지 않습니다."),
  ATTRIBUTE_DEFINITION_NOT_FOUND(HttpStatus.NOT_FOUND, "의상 속성 정의가 존재하지 않습니다."),
  ATTRIBUTE_DEFINITION_DUPLICATE(HttpStatus.CONFLICT, "이미 존재하는 의상 속성 정의입니다."),
  ATTRIBUTE_VALUE_NOT_FOUND(HttpStatus.NOT_FOUND, "의상 속성값이 존재하지 않습니다."),
  ATTRIBUTE_VALUE_DUPLICATE(HttpStatus.CONFLICT, "이미 존재하는 의상 속성값입니다."),
  IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "의상 이미지 업로드를 실패했습니다."),

  // --- Feed 관련 ErrorCode ---
  FEED_NOT_FOUND(HttpStatus.NOT_FOUND, "피드가 존재하지 않습니다."),

  // --- User 관련 ErrorCode ---
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "유저가 존재하지 않습니다."),
  PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "유저의 프로필이 존재하지 않습니다."),
  PROFILE_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "유저의 프로필 이미지가 존재하지 않습니다."),

  // --- Profile 관련 ErrorCode ---
  INVALID_PROFILE_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 이미지 타입입니다."),
  TOO_BIG_PROFILE_IMAGE(HttpStatus.BAD_REQUEST, "이미지의 크기가 너무 큽니다"),
  PROFILE_IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "프로필 이미지 업로드를 실패했습니다."),

  // --- Weather 관련 ErrorCode ---
  WEATHER_FORECAST_NOT_FOUND(HttpStatus.NOT_FOUND, "날씨 데이터가 존재하지 않습니다."),

  // --- Recommendation 관련 ErrorCode ---
  RECOMMENDATION_NOT_FOUND(HttpStatus.NOT_FOUND, "추천 데이터가 존재하지 않습니다.");



  private final HttpStatus status;
  private final String message;

  @Override
  public HttpStatus getStatus() {
    return this.status;
  }

  @Override
  public String getMessage() {
    return this.message;
  }

}

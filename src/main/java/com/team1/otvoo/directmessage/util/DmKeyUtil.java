package com.team1.otvoo.directmessage.util;

import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

public class DmKeyUtil {

  private static final String PREFIX = "direct-messages_";

  public static String generate(UUID userId1, UUID userId2) {
    if (userId1 == null || userId2 == null) {
      throw new RestException(ErrorCode.INVALID_INPUT_VALUE,
          Map.of("reason", "userId는 null일 수 없습니다."));
    }

    UUID[] sorted = {userId1, userId2};
    Arrays.sort(sorted);

    return PREFIX + sorted[0] + "_" + sorted[1];
  }

  public static UUID[] parse(String dmKey) {
    if (dmKey == null || !dmKey.startsWith(PREFIX)) {
      throw new RestException(ErrorCode.INVALID_INPUT_VALUE,
          Map.of("reason", "DM Key가 null이거나 형식이 올바르지 않습니다."));
    }

    String[] parts = dmKey.substring(PREFIX.length()).split("_");

    if (parts.length != 2) {
      throw new RestException(ErrorCode.INVALID_INPUT_VALUE,
          Map.of("reason", "DM Key는 두 개의 UUID로 구성되어야 합니다."));
    }

    try {
      UUID userId1 = UUID.fromString(parts[0]);
      UUID userId2 = UUID.fromString(parts[1]);

      UUID[] sorted = {userId1, userId2};
      Arrays.sort(sorted);
      return sorted;
    } catch (IllegalArgumentException e) {
      throw new RestException(ErrorCode.INVALID_INPUT_VALUE,
          Map.of("reason", "UUID 파싱 실패"));
    }
  }
}
package com.team1.otvoo.directmessage.util;

import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;

@Slf4j
public class DmKeyUtil {
  public static String generate(UUID userId1, UUID userId2) {
    if (userId1 == null || userId2 == null) {
      throw new RestException(ErrorCode.INVALID_INPUT_VALUE,
          Map.of("reason", "userId는 null일 수 없습니다."));
    }

    int compare = userId1.compareTo(userId2);
    UUID first = compare < 0 ? userId1 : userId2;
    UUID second = compare < 0 ? userId2 : userId1;

    String dmKey = first + "_" + second;
    log.debug("[DmKeyUtil] generate dmKey: {}", dmKey);
    return dmKey;
  }

  public static UUID[] parse(String dmKey) {
    if (dmKey == null) {
      throw new RestException(ErrorCode.INVALID_INPUT_VALUE, Map.of("reason", "DM Key가 null입니다."));}

    String[] parts = dmKey.split("_");

    if (parts.length != 2) {
      throw new RestException(ErrorCode.INVALID_INPUT_VALUE,
          Map.of("reason", "DM Key는 두 개의 UUID로 구성되어야 합니다."));
    }

    try {
      UUID userId1 = UUID.fromString(parts[0]);
      UUID userId2 = UUID.fromString(parts[1]);
      log.debug("[DmKeyUtil] parse dmKey: {} -> parsed: {}, {}", dmKey, userId1, userId2);

      return new UUID[]{userId1, userId2};
    } catch (IllegalArgumentException e) {
      throw new RestException(ErrorCode.INVALID_INPUT_VALUE,
          Map.of("reason", "UUID 파싱 실패"));
    }
  }
}
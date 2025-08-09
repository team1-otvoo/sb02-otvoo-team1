package com.team1.otvoo.directmessage.util;

import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class DmKeyUtilTest {

  @Test
  @DisplayName("dmKey 생성 성공 - 두 UUID 정렬 후 생성")
  void generate_shouldReturnSortedDmKey() {
    // given
    UUID userId1 = UUID.fromString("89a71b30-e73f-415f-b791-e8cb9694e5b6");
    UUID userId2 = UUID.fromString("96671e32-ff27-4215-bf96-d0575abc11f4");

    // when
    String dmKey = DmKeyUtil.generate(userId1, userId2);

    // then
    assertThat(dmKey).isEqualTo("89a71b30-e73f-415f-b791-e8cb9694e5b6_96671e32-ff27-4215-bf96-d0575abc11f4");
  }

  @Test
  @DisplayName("dmKey 파싱 성공 - 정렬된 UUID 배열 반환")
  void parse_shouldReturnSortedUuidArray() {
    // given
    String dmKey = "89a71b30-e73f-415f-b791-e8cb9694e5b6_96671e32-ff27-4215-bf96-d0575abc11f4";

    // when
    UUID[] result = DmKeyUtil.parse(dmKey);

    // then
    assertThat(result).hasSize(2);
    assertThat(result[0].toString()).isEqualTo("89a71b30-e73f-415f-b791-e8cb9694e5b6");
    assertThat(result[1].toString()).isEqualTo("96671e32-ff27-4215-bf96-d0575abc11f4");
  }

  @Test
  @DisplayName("dmKey 생성 실패 - 입력 UUID가 null")
  void generate_shouldThrowWhenUserIdIsNull() {
    // given
    UUID userId = UUID.randomUUID();

    // when & then
    assertThatThrownBy(() -> DmKeyUtil.generate(null, userId))
        .isInstanceOf(RestException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

    assertThatThrownBy(() -> DmKeyUtil.generate(userId, null))
        .isInstanceOf(RestException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
  }

  @Test
  @DisplayName("dmKey 파싱 실패 - null 또는 접두사 불일치")
  void parse_shouldThrowWhenDmKeyIsNullOrInvalidPrefix() {
    // when & then
    assertThatThrownBy(() -> DmKeyUtil.parse(null))
        .isInstanceOf(RestException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

    assertThatThrownBy(() -> DmKeyUtil.parse("invalid-prefix_abc_def"))
        .isInstanceOf(RestException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
  }

  @Test
  @DisplayName("dmKey 파싱 실패 - UUID 부분이 두 개가 아님")
  void parse_shouldThrowWhenDmKeyDoesNotContainTwoParts() {
    // given
    String invalidDmKey = "direct-messages_onlyonepart";

    // when & then
    assertThatThrownBy(() -> DmKeyUtil.parse(invalidDmKey))
        .isInstanceOf(RestException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
  }

  @Test
  @DisplayName("dmKey 파싱 실패 - UUID 변환 실패")
  void parse_shouldThrowWhenUuidParsingFails() {
    // given
    String invalidUuidDmKey = "direct-messages_invaliduuid_12345";

    // when & then
    assertThatThrownBy(() -> DmKeyUtil.parse(invalidUuidDmKey))
        .isInstanceOf(RestException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
  }
}
package com.team1.otvoo.directmessage.util;

import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class DmKeyUtilTest {

  @Test
  void generate_shouldReturnSortedDmKey() {
    UUID userId1 = UUID.fromString("89a71b30-e73f-415f-b791-e8cb9694e5b6");
    UUID userId2 = UUID.fromString("96671e32-ff27-4215-bf96-d0575abc11f4");

    String dmKey = DmKeyUtil.generate(userId1, userId2);

    assertThat(dmKey).isEqualTo("direct-messages_89a71b30-e73f-415f-b791-e8cb9694e5b6_96671e32-ff27-4215-bf96-d0575abc11f4");
  }

  @Test
  void parse_shouldReturnSortedUuidArray() {
    String dmKey = "direct-messages_89a71b30-e73f-415f-b791-e8cb9694e5b6_96671e32-ff27-4215-bf96-d0575abc11f4";

    UUID[] result = DmKeyUtil.parse(dmKey);

    assertThat(result).hasSize(2);
    assertThat(result[0].toString()).isEqualTo("89a71b30-e73f-415f-b791-e8cb9694e5b6");
    assertThat(result[1].toString()).isEqualTo("96671e32-ff27-4215-bf96-d0575abc11f4");
  }

  @Test
  void generate_shouldThrowWhenUserIdIsNull() {
    UUID userId = UUID.randomUUID();

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
  void parse_shouldThrowWhenDmKeyIsNullOrInvalidPrefix() {
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
  void parse_shouldThrowWhenDmKeyDoesNotContainTwoParts() {
    String invalidDmKey = "direct-messages_onlyonepart";

    assertThatThrownBy(() -> DmKeyUtil.parse(invalidDmKey))
        .isInstanceOf(RestException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
  }

  @Test
  void parse_shouldThrowWhenUuidParsingFails() {
    String invalidUuidDmKey = "direct-messages_invaliduuid_12345";

    assertThatThrownBy(() -> DmKeyUtil.parse(invalidUuidDmKey))
        .isInstanceOf(RestException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
  }
}
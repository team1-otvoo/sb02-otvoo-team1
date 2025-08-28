package com.team1.otvoo.clothes.entity;

import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import java.util.Map;

public enum ClothesType {
  TOP,
  BOTTOM,
  DRESS,
  OUTER,
  UNDERWEAR,
  ACCESSORY,
  SHOES,
  SOCKS,
  HAT,
  BAG,
  SCARF,
  ETC;

  public static ClothesType fromString(String type) {
    try {
      return ClothesType.valueOf(type);
    } catch (IllegalArgumentException e) {
      throw new RestException(
          ErrorCode.INVALID_INPUT_VALUE,
          Map.of("typeEqual", type)
      );
    }
  }
}

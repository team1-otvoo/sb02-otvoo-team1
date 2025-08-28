package com.team1.otvoo.user.mapper;

import java.lang.reflect.Field;

public class TestUtils {

  public static void setField(Object target, String fieldName, Object value) {
    try {
      Field field = getField(target.getClass(), fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static Field getField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
    // 부모 클래스까지 올라가며 탐색
    while (clazz != null) {
      try {
        return clazz.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        clazz = clazz.getSuperclass();
      }
    }
    throw new NoSuchFieldException("Field not found: " + fieldName);
  }
}

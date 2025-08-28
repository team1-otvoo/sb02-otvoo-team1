package com.team1.otvoo.config.converter;

import com.team1.otvoo.user.dto.SortBy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class SortByConverter implements Converter<String, SortBy> {
  @Override
  public SortBy convert(String source) {
    return SortBy.from(source); // 지금 enum의 from(String) 그대로 재사용
  }
}
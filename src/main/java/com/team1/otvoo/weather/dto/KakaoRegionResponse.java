package com.team1.otvoo.weather.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record KakaoRegionResponse(
    @JsonProperty("documents")
    List<Document> documents
) {
  public record Document(
      @JsonProperty("region_1depth_name")
      String region1,
      @JsonProperty("region_2depth_name")
      String region2,
      @JsonProperty("region_3depth_name")
      String region3
  ) {}
}

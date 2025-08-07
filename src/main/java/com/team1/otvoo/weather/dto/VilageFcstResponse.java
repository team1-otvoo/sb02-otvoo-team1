package com.team1.otvoo.weather.dto;

import java.util.List;
import lombok.Data;

// 기상청 단기예보 조회 API(getVilageFcst) 응답 DTO
@Data
public class VilageFcstResponse {

  private ResponseHeader header;
  private ResponseBody body;

  @Data
  public static class ResponseHeader {
    private String resultCode;
    private String resultMsg;
  }

  @Data
  public static class ResponseBody {
    private Items items;
  }

  @Data
  public static class Items {
    private List<FcstItem> item;
  }

  @Data
  public static class FcstItem {
    private String baseDate;
    private String baseTime;
    private String category;
    private String fcstDate;
    private String fcstTime;
    private String fcstValue;
    private int nx;
    private int ny;

    public FcstItem(String baseDate, String baseTime,
        String fcstDate, String fcstTime,
        String category, String fcstValue,
        int nx, int ny) {
      this.baseDate  = baseDate;
      this.baseTime  = baseTime;
      this.fcstDate  = fcstDate;
      this.fcstTime  = fcstTime;
      this.category  = category;
      this.fcstValue = fcstValue;
      this.nx        = nx;
      this.ny        = ny;
    }
  }
}

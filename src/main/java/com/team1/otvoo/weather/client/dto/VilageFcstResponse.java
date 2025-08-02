package com.team1.otvoo.weather.client.dto;

import java.util.List;
import lombok.Data;

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
  }
}

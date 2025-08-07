package com.team1.otvoo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "profile.default-image")
@Getter
@Setter
public class DefaultProfileImageProperties {
  private String url;
  private String filename;
  private String contentType;
  private Long size;
  private Integer width;
  private Integer height;
}

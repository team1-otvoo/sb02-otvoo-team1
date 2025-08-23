package com.team1.otvoo.config.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "aws.s3")
@Getter
@Setter
public class S3Props {
  private String bucket;
  private String region;
  private String accessKey;
  private String secretKey;
  private long presignedExpirationSeconds;
}
package com.team1.otvoo.config;

import com.team1.otvoo.config.props.S3Props;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration

public class S3StorageConfig {
  @Bean(destroyMethod = "close")
  public S3Client s3Client(S3Props props) {
    return S3Client.builder()
        .region(Region.of(props.getRegion()))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())
            )
            // 운영 권장: DefaultCredentialsProvider.create()
        )
        .build();
  }

  @Bean(destroyMethod = "close")
  public S3Presigner s3Presigner(S3Props props) {
    return S3Presigner.builder()
        .region(Region.of(props.getRegion()))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())
            )
            // 운영 권장: DefaultCredentialsProvider.create()
        )
        .build();
  }
}
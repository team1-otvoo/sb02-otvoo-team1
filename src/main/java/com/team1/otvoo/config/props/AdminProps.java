package com.team1.otvoo.config.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "admin")
@Getter
@Setter
public class AdminProps {
  private String name;
  private String email;
  private String password;
}

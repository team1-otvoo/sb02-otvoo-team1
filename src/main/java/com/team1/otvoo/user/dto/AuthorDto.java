package com.team1.otvoo.user.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@Builder
public class AuthorDto {
  UUID userId;
  String name;
  String profileImageUrl;
}

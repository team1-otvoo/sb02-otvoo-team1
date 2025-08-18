package com.team1.otvoo.notification.entity;

import java.util.Arrays;
import java.util.MissingFormatArgumentException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public enum NotificationType {
  ROLE_CHANGE("내 권한이 변경되었어요.","내 권한이 [%s]에서 [%s]로 변경되었어요."),
  ADD_ATTRIBUTION("새로운 의상 속성이 추가되었어요.", "내 의상에 [%s] 속성을 추가해보세요."),
  UPDATE_ATTRIBUTION("의상 속성이 변경되었어요.", "[[수정]%s] 속성을 확인해보세요."),
  LIKE_MY_FEED("%s님이 내 피드를 좋아합니다.", ""),
  COMMENT_ON_MY_FEED("%s님이 댓글을 달았어요.", "%s"),
  FOLLOWEE_ADD_FEED("%s님이 새로운 피드를 작성했어요.", "%s"),
  FOLLOW("%s님이 나를 팔로우했어요.", ""),
  RECEIVE_DM("[DM] %s", "%s");

  private final String title;
  private final String content;

  NotificationType(String title, String content) {
    this.title = title;
    this.content = content;
  }

  public String formatTitle(String... args) {
    try {
      return String.format(title, (Object[]) args);
    } catch (MissingFormatArgumentException e) {
      log.warn("NotificationType title format error: type={}, args={}", this.name(), Arrays.toString(args));
      return title;
    }
  }

  public String formatContent(String... args) {
    try {
      return String.format(content, (Object[]) args);
    } catch (MissingFormatArgumentException e) {
      log.warn("NotificationType content format error: type={}, args={}", this.name(), Arrays.toString(args));
      return content;
    }
  }

}

package com.team1.otvoo.notification.service;

import com.team1.otvoo.clothes.entity.ClothesAttributeDefinition;
import com.team1.otvoo.comment.entity.FeedComment;
import com.team1.otvoo.directmessage.entity.DirectMessage;
import com.team1.otvoo.feed.entity.Feed;
import com.team1.otvoo.feed.entity.FeedLike;
import com.team1.otvoo.user.entity.Role;
import com.team1.otvoo.user.entity.User;

public interface SendNotificationService {
  void sendUserRoleNotification(Role previousUserRole, User user);
  void sendClothesAttributeNotification(String methodType, ClothesAttributeDefinition clothesAttributeDefinition);
  void sendLikeNotification(FeedLike feedLike);
  void sendCommentNotification(FeedComment feedComment);
  void sendFeedNotification(Feed feed);
  void sendFollowNotification(User follower, User receiver);
  void sendDirectMessageNotification(DirectMessage directMessage);
  //void sendWeatherForecastNotifiaction();
}

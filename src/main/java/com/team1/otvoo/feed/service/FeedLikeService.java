package com.team1.otvoo.feed.service;

import java.util.UUID;

public interface FeedLikeService {
  void create(UUID feedId);
  void delete(UUID feedId);

}

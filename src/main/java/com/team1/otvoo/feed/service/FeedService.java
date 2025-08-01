package com.team1.otvoo.feed.service;

import com.team1.otvoo.feed.dto.FeedCreateRequest;
import com.team1.otvoo.feed.dto.FeedDto;

public interface FeedService {
  FeedDto create(FeedCreateRequest request);

}

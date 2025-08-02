package com.team1.otvoo.feed.service;

import com.team1.otvoo.feed.dto.FeedCreateRequest;
import com.team1.otvoo.feed.dto.FeedDto;
import com.team1.otvoo.feed.dto.FeedUpdateRequest;
import java.util.UUID;

public interface FeedService {
  FeedDto create(FeedCreateRequest request);

  FeedDto update(UUID id, FeedUpdateRequest request);

  void delete(UUID id);

}

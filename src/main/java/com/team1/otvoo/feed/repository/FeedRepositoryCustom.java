package com.team1.otvoo.feed.repository;

import com.team1.otvoo.feed.dto.FeedDto;
import com.team1.otvoo.feed.dto.FeedSearchCondition;
import java.util.UUID;
import org.springframework.data.domain.Slice;

public interface FeedRepositoryCustom {
  Slice<FeedDto> searchByCondition(FeedSearchCondition condition, UUID currentUserId);
}

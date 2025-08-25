package com.team1.otvoo.feed.elasticsearch.service;

import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.feed.dto.FeedDto;
import com.team1.otvoo.feed.dto.FeedSearchCondition;
import com.team1.otvoo.feed.elasticsearch.repository.FeedSearchRepository;
import com.team1.otvoo.feed.repository.FeedLikeRepository;
import com.team1.otvoo.feed.repository.FeedRepository;
import com.team1.otvoo.security.CustomUserDetails;
import com.team1.otvoo.user.repository.ProfileRepository;
import com.team1.otvoo.user.resolver.ProfileImageUrlResolver;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedElasticSearchService {

  private final FeedSearchRepository feedSearchRepository;
  private final FeedRepository feedRepository;
  private final FeedLikeRepository feedLikeRepository;
  private final ProfileImageUrlResolver profileImageUrlResolver;
  private final ProfileRepository profileRepository;

  public Slice<FeedDto> getFeedsWithCursor(FeedSearchCondition searchCondition) {
    Slice<FeedDto> feedDtoSlice = feedSearchRepository.searchFeeds(searchCondition);
    UUID userId = ((CustomUserDetails) (SecurityContextHolder.getContext().getAuthentication()
        .getPrincipal())).getUser().getId();
    feedDtoSlice.forEach(
        fs -> {
          fs.setCommentCount(feedRepository.findById(fs.getId())
              .orElseThrow(
                  () -> new RestException(ErrorCode.FEED_NOT_FOUND, Map.of("feedId", fs.getId())))
              .getCommentCount());
          fs.setLikedByMe(
              feedLikeRepository.existsFeedLikeByFeed_IdAndLikedBy_Id(fs.getId(), userId));
          UUID profileId = profileRepository.findByUserId(fs.getAuthor().getUserId()).orElseThrow(
              () -> new RestException(ErrorCode.PROFILE_NOT_FOUND,
                  Map.of("userId", fs.getAuthor().getUserId()))).getId();
          fs.getAuthor().setProfileImageUrl(profileImageUrlResolver.resolve(profileId));
        }
    );
    return feedDtoSlice;
  }
}

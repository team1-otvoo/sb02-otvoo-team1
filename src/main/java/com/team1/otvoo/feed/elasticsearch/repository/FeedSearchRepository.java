package com.team1.otvoo.feed.elasticsearch.repository;

import com.team1.otvoo.feed.elasticsearch.document.FeedDocument;
import java.util.UUID;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface FeedSearchRepository extends ElasticsearchRepository<FeedDocument, UUID>, FeedSearchRepositoryCustom {

}

package com.team1.otvoo.feed.repository;

import com.team1.otvoo.feed.entity.FeedClothes;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedClothesRepository extends JpaRepository<FeedClothes, UUID> {
  @Query("select fc from FeedClothes fc " +
      "join fetch fc.clothes c " +
      "left join fetch c.selectedValues sv " +
      "left join fetch sv.definition def " +
      "left join fetch def.values " +
      "where fc.feed.id in :feedIds")
  List<FeedClothes> findAllByFeedIdInWithClothesAndSelectedValues(@Param("feedIds") List<UUID> feedIds);

  @Query("select fc from FeedClothes fc "
      + "where fc.clothes.id in :clothesIds")
  List<FeedClothes> findAllByClothesId(@Param("clothesIds") List<UUID> clothesIds);

  List<FeedClothes> findByClothes_Id(UUID clothesId);
}

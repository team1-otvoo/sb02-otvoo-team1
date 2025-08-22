package com.team1.otvoo.feed.repository;

import com.team1.otvoo.clothes.entity.Clothes;
import com.team1.otvoo.clothes.entity.ClothesAttributeDefinition;
import com.team1.otvoo.clothes.entity.ClothesAttributeValue;
import com.team1.otvoo.clothes.entity.ClothesSelectedValue;
import com.team1.otvoo.clothes.entity.ClothesType;
import com.team1.otvoo.clothes.repository.ClothesAttributeDefRepository;
import com.team1.otvoo.clothes.repository.ClothesRepository;
import com.team1.otvoo.common.AbstractPostgresTest;
import com.team1.otvoo.config.QueryDslConfig;
import com.team1.otvoo.feed.entity.Feed;
import com.team1.otvoo.feed.entity.FeedClothes;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@DataJpaTest
@Import(QueryDslConfig.class)
public class FeedClothesRepositoryTest extends AbstractPostgresTest {
  @Autowired
  FeedClothesRepository feedClothesRepository;

  @Autowired
  FeedRepository feedRepository;

  @Autowired
  UserRepository userRepository;

  @Autowired
  ClothesRepository clothesRepository;

  @Autowired
  ClothesAttributeDefRepository clothesAttributeDefRepository;

  @Autowired
  EntityManager entityManager;

  @Test
  @DisplayName("FeedClothes with fetch join 테스트")
  void testFindAllByFeedIdInWithClothesAndSelectedValues() {
    // 저장에 필요한 엔티티 생성 및 저장

    User user = User.builder()
        .email("test@test.com")
        .password("password")
        .build();
    userRepository.save(user);

    ClothesAttributeValue value = new ClothesAttributeValue("두꺼움");
    ClothesAttributeDefinition definition = new ClothesAttributeDefinition("두께감", List.of(value));
    clothesAttributeDefRepository.save(definition);

    Clothes clothes = new Clothes(user, "티셔츠", ClothesType.TOP, List.of());
    ClothesSelectedValue selectedValue = new ClothesSelectedValue(definition, value);
    ReflectionTestUtils.setField(selectedValue, "clothes", clothes);
    ReflectionTestUtils.setField(clothes, "selectedValues", List.of(selectedValue));
    clothesRepository.save(clothes);

    Feed feed = Feed.builder()
        .user(user)
        .content("테스트 피드")
        .build();
    feedRepository.save(feed);

    FeedClothes feedClothes = new FeedClothes(feed, clothes);
    ReflectionTestUtils.setField(feed, "feedClothes", List.of(feedClothes));

    entityManager.persist(feedClothes);

    entityManager.flush();
    entityManager.clear();

    // when
    List<FeedClothes> result = feedClothesRepository.findAllByFeedIdInWithClothesAndSelectedValues(List.of(feed.getId()));

    PersistenceUnitUtil util = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();

    // then
    assertThat(result).isNotEmpty();
    FeedClothes loaded = result.get(0);

    // fetch join 되는 필드가 잘 로딩됐는지 검사
    assertThat(util.isLoaded(loaded, "clothes")).isTrue();
    assertThat(util.isLoaded(loaded.getClothes(), "selectedValues")).isTrue();
    assertThat(loaded.getClothes().getSelectedValues()).isNotEmpty();
    assertThat(util.isLoaded(loaded.getClothes().getSelectedValues().get(0), "definition")).isTrue();
    assertThat(util.isLoaded(loaded.getClothes().getSelectedValues().get(0).getDefinition(), "values")).isTrue();
  }
}

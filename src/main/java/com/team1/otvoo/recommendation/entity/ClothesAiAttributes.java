package com.team1.otvoo.recommendation.entity;

import com.team1.otvoo.clothes.entity.Clothes;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "clothes_ai_attributes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ClothesAiAttributes {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @OneToOne
  @JoinColumn(name = "clothes_id", nullable = false)
  private Clothes clothes;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "attributes", columnDefinition = "jsonb", nullable = false)
  private Map<String, String> attributes = new HashMap<>();

  public ClothesAiAttributes(Clothes clothes, Map<String, String> attributes) {
    this.clothes = clothes;
    this.attributes = attributes;
  }
}

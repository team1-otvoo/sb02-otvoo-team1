package com.team1.otvoo.clothes.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "clothes_selected_values")
@Getter
@NoArgsConstructor
public class ClothesSelectedValue {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "clothes_id", nullable = false)
  private Clothes clothes;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "definition_id", nullable = false)
  private ClothesAttributeDefinition definition;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "value_id", nullable = false)
  private ClothesAttributeValue value;

  public ClothesSelectedValue(Clothes clothes, ClothesAttributeDefinition definition,
      ClothesAttributeValue value) {
    this.clothes = clothes;
    this.definition = definition;
    this.value = value;
  }

  protected void setClothes(Clothes clothes) {
    this.clothes = clothes;
  }
}

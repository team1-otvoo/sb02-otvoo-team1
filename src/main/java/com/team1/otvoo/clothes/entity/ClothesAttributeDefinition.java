package com.team1.otvoo.clothes.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "clothes_attribute_definitions")
@Getter
@NoArgsConstructor
public class ClothesAttributeDefinition {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true)
  private String name;

  @OneToMany(mappedBy = "definition", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderColumn(name = "order_index")
  private List<ClothesAttributeValue> values = new ArrayList<>();

  public ClothesAttributeDefinition(String name, List<ClothesAttributeValue> values) {
    this.name = name;
    for (ClothesAttributeValue value : values) {
      addValue(value);
    }
  }

  public void addValue(ClothesAttributeValue value) {
    values.add(value);
    value.setDefinition(this);
  }

  public void update(String newName) {
      this.name = newName;
  }
}

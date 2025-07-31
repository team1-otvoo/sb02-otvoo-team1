package com.team1.otvoo.clothes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "clothes_attribute_values",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"definition_id", "value"})
    })
@Getter
@NoArgsConstructor
public class ClothesAttributeValue {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "definition_id", nullable = false)
  private ClothesAttributeDefinition definition;

  @Column(nullable = false)
  private String value;
}

package com.team1.otvoo.clothes.extraction.candidate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "attribute_candidates",
    uniqueConstraints = @UniqueConstraint(columnNames = {"definition", "value"})
)

@Getter
@NoArgsConstructor
public class AttributeCandidate {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private String definition;

  @Column(nullable = false)
  private String value;

  @Column(nullable = false)
  private int count = 1;

  public AttributeCandidate(String definition, String value) {
    this.definition = definition;
    this.value = value;
  }

  public void addCount() {
    this.count += 1;
  }


}

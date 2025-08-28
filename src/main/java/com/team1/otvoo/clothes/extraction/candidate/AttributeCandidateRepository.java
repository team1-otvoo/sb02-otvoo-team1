package com.team1.otvoo.clothes.extraction.candidate;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttributeCandidateRepository extends JpaRepository<AttributeCandidate, UUID> {

  Optional<AttributeCandidate> findByDefinitionAndValue(String definition, String value);

}

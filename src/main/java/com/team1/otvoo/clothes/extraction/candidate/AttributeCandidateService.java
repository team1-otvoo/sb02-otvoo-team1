package com.team1.otvoo.clothes.extraction.candidate;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttributeCandidateService {

  private final AttributeCandidateRepository attributeCandidateRepository;

  @Transactional
  public void record(String definition, String value) {
    attributeCandidateRepository.findByDefinitionAndValue(definition, value)
        .ifPresentOrElse(AttributeCandidate::addCount,
            () -> attributeCandidateRepository.save(new AttributeCandidate(definition, value))
        );
  }

  @Transactional
  public void recordAll(Map<String, String> unknowns) {
    if (unknowns == null || unknowns.isEmpty()) {
      return;
    }
    unknowns.forEach(this::record);
  }
}

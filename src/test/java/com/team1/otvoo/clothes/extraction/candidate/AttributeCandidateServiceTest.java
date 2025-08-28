package com.team1.otvoo.clothes.extraction.candidate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AttributeCandidateServiceTest {

  @Mock
  private AttributeCandidateRepository attributeCandidateRepository;

  @InjectMocks
  private AttributeCandidateService attributeCandidateService;

  @Test
  @DisplayName("기존 데이터가 없는 경우 새 엔티티 저장")
  void record_NewCandidate_Success() {
    // given
    when(attributeCandidateRepository.findByDefinitionAndValue("두께감", "두꺼움"))
        .thenReturn(Optional.empty());

    attributeCandidateService.record("두께감", "두꺼움");

    verify(attributeCandidateRepository).save(argThat(candidate ->
        candidate.getDefinition().equals("두께감") &&
            candidate.getValue().equals("두꺼움") &&
            candidate.getCount() == 1
    ));
  }

  @Test
  @DisplayName("기존 데이터가 있는 경우 addCount 호출")
  void record_ExistingCandidate_AddCount_Success() {
    // given
    String def = "두께감";
    String val = "두꺼움";
    AttributeCandidate existing = spy(new AttributeCandidate(def, val));

    when(attributeCandidateRepository.findByDefinitionAndValue(def, val))
        .thenReturn(Optional.of(existing));

    // when
    attributeCandidateService.record(def, val);

    // then
    verify(existing).addCount();
    verify(attributeCandidateRepository, never()).save(any());
  }

  @Test
  @DisplayName("여러 데이터가 들어오면 모두 처리")
  void recordAll_Success(){
    // given
    String def1 = "색상";
    String val1 = "블랙";
    String def2 = "두께감";
    String val2 = "두꺼움";

    when(attributeCandidateRepository.findByDefinitionAndValue(def1,val1))
        .thenReturn(Optional.empty());
    when(attributeCandidateRepository.findByDefinitionAndValue(def2,val2))
        .thenReturn(Optional.empty());

    // when
    attributeCandidateService.recordAll(Map.of(
        "색상","블랙",
        "두께감","두꺼움"
    ));

    // then
    verify(attributeCandidateRepository,times(2)).save(any(AttributeCandidate.class));
  }
}
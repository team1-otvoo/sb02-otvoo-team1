package com.team1.otvoo.clothes.extraction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.team1.otvoo.clothes.dto.ClothesDto;
import com.team1.otvoo.clothes.entity.ClothesAttributeDefinition;
import com.team1.otvoo.clothes.entity.ClothesAttributeValue;
import com.team1.otvoo.clothes.entity.ClothesType;
import com.team1.otvoo.clothes.extraction.candidate.AttributeCandidateService;
import com.team1.otvoo.clothes.extraction.dto.AnalyzeResponse;
import com.team1.otvoo.clothes.extraction.dto.ClassificationResult;
import com.team1.otvoo.clothes.extraction.dto.HtmlSlices;
import com.team1.otvoo.clothes.extraction.pipeline.AttributeClassifier;
import com.team1.otvoo.clothes.extraction.pipeline.ClothesInfoExtractor;
import com.team1.otvoo.clothes.extraction.pipeline.HtmlSliceParser;
import com.team1.otvoo.clothes.repository.ClothesAttributeDefRepository;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ClothesExtractServiceImplTest {

  @Mock
  private ClothesInfoExtractor clothesInfoExtractor;
  @Mock
  private HtmlSliceParser htmlSliceParser;
  @Mock
  private AttributeClassifier attributeClassifier;
  @Mock
  private ClothesAttributeDefRepository attributeDefRepository;
  @Mock
  private AttributeCandidateService attributeCandidateService;

  @InjectMocks
  private ClothesExtractServiceImpl clothesExtractService;

  @Test
  @DisplayName("의상 추출 성공_LLM 응답 성공")
  void extractClothesInfo_Success_WithLLM() throws Exception {
    // given
    String url = "https://shop.example/item/123";

    try (MockedStatic<Jsoup> jsoup = Mockito.mockStatic(Jsoup.class)) {
      Connection conn = mock(Connection.class);
      Document doc = Jsoup.parse("<html><head><title>t</title></head><body></body></html>");
      jsoup.when(() -> Jsoup.connect(url)).thenReturn(conn);
      when(conn.userAgent(anyString())).thenReturn(conn);
      when(conn.timeout(anyInt())).thenReturn(conn);
      when(conn.maxBodySize(anyInt())).thenReturn(conn);
      when(conn.followRedirects(true)).thenReturn(conn);
      when(conn.get()).thenReturn(doc);

      HtmlSlices slices = mock(HtmlSlices.class);
      when(htmlSliceParser.buildSlices(doc, url)).thenReturn(slices);

      AnalyzeResponse analyze = mock(AnalyzeResponse.class);
      when(analyze.name()).thenReturn("코트");
      when(analyze.imageUrl()).thenReturn("/img/coat.jpg");
      when(analyze.type()).thenReturn(ClothesType.OUTER);
      when(analyze.attributes()).thenReturn(Map.of("색상", "블랙", "두께감", "두꺼움"));
      when(clothesInfoExtractor.analyze(eq(slices), any(String[].class))).thenReturn(analyze);

      when(htmlSliceParser.normalize("코트")).thenReturn("코트");
      when(htmlSliceParser.validateAndAbsolutize(url, "/img/coat.jpg"))
          .thenReturn("https://shop.example/item/coat.jpg");

      ClassificationResult classificationResult = mock(ClassificationResult.class);
      when(classificationResult.recognized()).thenReturn(Map.of("색상", "블랙"));
      when(classificationResult.unknowns()).thenReturn(Map.of("두께감", "두꺼움"));
      when(attributeClassifier.classify(any())).thenReturn(classificationResult);

      ClothesAttributeDefinition definition = mock(ClothesAttributeDefinition.class);
      UUID definitionId = UUID.randomUUID();
      when(definition.getId()).thenReturn(definitionId);
      when(definition.getName()).thenReturn("색상");

      ClothesAttributeValue value1 = mock(ClothesAttributeValue.class);
      when(value1.getValue()).thenReturn("블랙");
      ClothesAttributeValue value2 = mock(ClothesAttributeValue.class);
      when(value2.getValue()).thenReturn("그레이");
      when(definition.getValues()).thenReturn(List.of(value1, value2));
      when(attributeDefRepository.findAllWithValues()).thenReturn(List.of(definition));

      // when
      ClothesDto dto = clothesExtractService.extract(url);

      //then
      assertThat(dto.name()).isEqualTo("코트");
      assertThat(dto.imageUrl()).isEqualTo("https://shop.example/item/coat.jpg");
      assertThat(dto.type()).isEqualTo(ClothesType.OUTER);

      assertThat(dto.attributes()).hasSize(1);
      assertThat(dto.attributes().get(0).definitionId()).isEqualTo(definitionId);
      assertThat(dto.attributes().get(0).definitionName()).isEqualTo("색상");
      assertThat(dto.attributes().get(0).selectableValues()).containsExactly("블랙", "그레이");
      assertThat(dto.attributes().get(0).value()).isEqualTo("블랙");

      verify(attributeCandidateService).recordAll(anyMap());
    }
  }

  @Test
  @DisplayName("의상 추출 성공_LLM 응답 없음(fallback 경로)")
  void extractClothesInfo_Success_WithFallback() throws Exception {
    String url = "https://shop.example/item/456";

    try (MockedStatic<Jsoup> jsoup = Mockito.mockStatic(Jsoup.class)) {
      Connection conn = mock(Connection.class);
      Document doc = Jsoup.parse("<html><head><title>t</title></head><body></body></html>");
      jsoup.when(() -> Jsoup.connect(url)).thenReturn(conn);
      when(conn.userAgent(anyString())).thenReturn(conn);
      when(conn.timeout(anyInt())).thenReturn(conn);
      when(conn.maxBodySize(anyInt())).thenReturn(conn);
      when(conn.followRedirects(true)).thenReturn(conn);
      when(conn.get()).thenReturn(doc);

      HtmlSlices slices = mock(HtmlSlices.class);
      when(htmlSliceParser.buildSlices(doc, url)).thenReturn(slices);

      when(clothesInfoExtractor.analyze(eq(slices), any(String[].class))).thenReturn(null);

      when(htmlSliceParser.fallbackName(doc)).thenReturn("기본 이름");
      when(htmlSliceParser.normalize("기본 이름")).thenReturn("기본 이름");
      when(htmlSliceParser.fallbackImageUrl(doc, url, slices))
          .thenReturn("https://img.example/fallback.jpg");

      ClothesAttributeDefinition def = mock(ClothesAttributeDefinition.class);
      when(def.getId()).thenReturn(UUID.randomUUID());
      when(def.getName()).thenReturn("색상");
      ClothesAttributeValue v1 = mock(ClothesAttributeValue.class);
      when(v1.getValue()).thenReturn("블랙");
      ClothesAttributeValue v2 = mock(ClothesAttributeValue.class);
      when(v2.getValue()).thenReturn("그레이");
      when(def.getValues()).thenReturn(List.of(v1, v2));
      when(attributeDefRepository.findAllWithValues()).thenReturn(List.of(def));

      // when
      ClothesDto dto = clothesExtractService.extract(url);

      // then
      assertThat(dto.name()).isEqualTo("기본 이름");
      assertThat(dto.imageUrl()).isEqualTo("https://img.example/fallback.jpg");
      assertThat(dto.type()).isNull(); // LLM 없음 → 타입도 없음
      assertThat(dto.attributes()).hasSize(1);
      // 인식값이 없으니 빈 문자열
      assertThat(dto.attributes().get(0).value()).isEqualTo("");

      verify(attributeClassifier, never()).classify(any());
      verify(attributeCandidateService, never()).recordAll(any());
    }
  }

  @Test
  @DisplayName("의상 추출 실패_http/https가 아닌 URL")
  void extractClothesInfo_Fail_InvalidScheme() {
    // given
    String badUrl = "xxx://shop.example/item/999";

    // when & then
    assertThatThrownBy(() -> clothesExtractService.extract(badUrl))
        .isInstanceOf(RestException.class)
        .hasMessage(ErrorCode.INVALID_INPUT_VALUE.getMessage());

    verifyNoInteractions(clothesInfoExtractor, htmlSliceParser, attributeClassifier,
        attributeDefRepository, attributeCandidateService);
  }
}
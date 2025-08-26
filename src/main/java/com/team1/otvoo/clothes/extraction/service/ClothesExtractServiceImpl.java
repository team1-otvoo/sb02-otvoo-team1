package com.team1.otvoo.clothes.extraction.service;

import static java.util.Arrays.stream;

import com.team1.otvoo.clothes.dto.ClothesDto;
import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeWithDefDto;
import com.team1.otvoo.clothes.entity.ClothesAttributeDefinition;
import com.team1.otvoo.clothes.entity.ClothesAttributeValue;
import com.team1.otvoo.clothes.extraction.candidate.AttributeCandidateService;
import com.team1.otvoo.clothes.extraction.dto.ClassificationResult;
import com.team1.otvoo.clothes.extraction.dto.HtmlSlices;
import com.team1.otvoo.clothes.extraction.dto.AnalyzeResponse;
import com.team1.otvoo.clothes.entity.ClothesType;
import com.team1.otvoo.clothes.extraction.pipeline.AttributeClassifier;
import com.team1.otvoo.clothes.extraction.pipeline.ClothesInfoExtractor;
import com.team1.otvoo.clothes.extraction.pipeline.HtmlSliceParser;
import com.team1.otvoo.clothes.repository.ClothesAttributeDefRepository;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClothesExtractServiceImpl implements ClothesExtractService {

  private static final int HTTP_TIMEOUT_MS = 7000; // 7초
  private static final int MAX_BODY_SIZE_BYTES = 5000000;   // 5MB
  private static final String USER_AGENT = "Mozilla/5.0 (OtvooBot/1.0)"; // 브라우저처럼 보이되 우리 식별자 포함
  private static final String[] ALLOWED_TYPES =
      stream(ClothesType.values()).map(Enum::name).toArray(String[]::new);

  private final ClothesInfoExtractor clothesInfoExtractor;
  private final HtmlSliceParser parser;
  private final AttributeClassifier attributeClassifier;
  private final AttributeCandidateService attributeCandidateService;
  private final ClothesAttributeDefRepository clothesAttributeDefRepository;

  @Override
  @Transactional
  public ClothesDto extract(String url) {
    // [보안] http/https 스킴만 허용
    String scheme = safeScheme(url);
    if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
      throw new RestException(
          ErrorCode.INVALID_INPUT_VALUE,
          Map.of("url", "http 나 https 로 시작하는 url이 필요합니다.")
      );
    }

    // 1) HTML 다운로드(타임아웃/사이즈 제한/리다이렉트 허용)
    final Document doc;
    try {
      doc = Jsoup.connect(url)
          .userAgent(USER_AGENT)
          .timeout(HTTP_TIMEOUT_MS)
          .maxBodySize(MAX_BODY_SIZE_BYTES)
          .followRedirects(true)
          .get();
    } catch (Exception e) {
      log.warn("패치 실패 url={} msg={}", url, e.getMessage());
      throw new RestException(ErrorCode.FETCH_OR_PARSE_FAILED, Map.of("url", url));
    }

    // 2) 파싱: html -> htmlSlices (원문 JSON들/이미지 후보 등)
    HtmlSlices slices = parser.buildSlices(doc, url);

    // 3) LLM 호출
    AnalyzeResponse response = null;
    try {
      response = clothesInfoExtractor.analyze(slices, ALLOWED_TYPES);
    } catch (Exception e) {
      log.warn("LLM 추출 실패 url={}", url, e);
    }

    // 4) 결과 정리
    String name = parser.normalize((response != null && response.name() != null)
        ? response.name()
        : parser.fallbackName(doc)
    );

    String imageUrl = (response != null && response.imageUrl() != null)
        ? parser.validateAndAbsolutize(url, response.imageUrl())
        : parser.fallbackImageUrl(doc, url, slices);

    ClothesType type = (response != null ? response.type() : null);

    ClassificationResult classification =
        (response != null && response.attributes() != null)
            ? attributeClassifier.classify(response.attributes())
            : null;

    Map<String, String> recognizedAttributes =
        (classification != null && classification.recognized() != null)
            ? classification.recognized() : Collections.emptyMap();

    List<ClothesAttributeDefinition> definitions = clothesAttributeDefRepository.findAllWithValues();

    List<ClothesAttributeWithDefDto> attributes = definitions.stream()
        .map(def -> new ClothesAttributeWithDefDto(
            def.getId(),
            def.getName(),
            def.getValues().stream().map(ClothesAttributeValue::getValue).toList(),
            recognizedAttributes.getOrDefault(def.getName(), "")
        ))
        .toList();

    if (classification != null && classification.unknowns() != null) {
      attributeCandidateService.recordAll(classification.unknowns());
    }

    // 5) DTO 반환(빈 문자열은 null로 통일)
    return new ClothesDto(
        null,
        null,
        emptyToNull(name),
        emptyToNull(imageUrl),
        type,
        attributes,
        null
    );
  }

  /**
   * 안전하게 스킴만 추출(실패 시 null)
   */
  private String safeScheme(String inputUrl) {
    try {
      return URI.create(inputUrl).getScheme();
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * 빈 문자열은 null로 교체
   */
  private String emptyToNull(String s) {
    return (s == null || s.isEmpty()) ? null : s;
  }
}
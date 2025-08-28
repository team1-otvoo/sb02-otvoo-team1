package com.team1.otvoo.clothes.extraction.pipeline;

import com.team1.otvoo.clothes.extraction.dto.HtmlSlices;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HtmlSliceParser {

  private static final int MAX_JSON_PRIMARY = 20000;
  private static final int MAX_JSON_LD = 10000;
  private static final int MAX_IMAGE_CANDIDATES = 15;

  public HtmlSlices buildSlices(Document doc, String baseUrl) {
    String title = normalize(doc.title());     // <title> (이름 정제에 힌트)
    String h1 = firstText(doc, "h1");       // 본문 대표 제목(있으면)

    // 1순위: id로 직접 찾기(__NEXT_DATA__ → _NEXT_DATA_ → __NUXT__ → 첫 ld+json)
    String primaryJson = firstDataByAttrValue(doc, "id", "__NEXT_DATA__");
    if (primaryJson == null) {
      primaryJson = firstDataByAttrValue(doc, "id", "_NEXT_DATA_");
    }
    if (primaryJson == null) {
      primaryJson = firstDataByAttrValue(doc, "id", "__NUXT__");
    }
    if (primaryJson == null) {
      primaryJson = firstDataByAttrValue(doc, "type", "application/ld+json");
    }

    // 2순위: 보조 JSON: ld+json 전부(최대 3개, 각 10k자) → LLM 힌트 강화
    List<String> extraJsons = new ArrayList<>();
    Elements ldAll = doc.getElementsByAttributeValue("type", "application/ld+json");
    int count = 0;
    for (Element el : ldAll) {
      String raw = safeTrim(el.data());                // script 원문은 data()로 읽기
      if (raw == null) {
        continue;
      }
      if (raw.length() > MAX_JSON_LD) {
        raw = raw.substring(0, MAX_JSON_LD);
      }
      extraJsons.add(raw);
      if (++count >= 3) {
        break;
      }
    }

    // 이미지 후보 수집(전부 절대경로 보정)
    List<String> imageCandidates = collectImageCandidates(doc, baseUrl, MAX_IMAGE_CANDIDATES);

    return new HtmlSlices(
        baseUrl,
        title,
        h1,
        primaryJson,
        extraJsons,
        imageCandidates
    );
  }


  /**
   * LLM name 실패 시 폴백 이름(og:title → twitter:title → meta[name=title] → h1 → <title>)
   */
  public String fallbackName(Document doc) {
    return firstNonBlank(
        meta(doc, "meta[property=og:title]", "content"),
        meta(doc, "meta[name=twitter:title]", "content"),
        meta(doc, "meta[name=title]", "content"),
        firstText(doc, "h1"),
        normalize(doc.title())
    );
  }

  /**
   * LLM image 실패 시 폴백 이미지(og/twitter → slice imageCandidates)
   */
  public String fallbackImageUrl(Document doc, String baseUrl, HtmlSlices slices) {
    return firstNonBlank(
        validateAndAbsolutize(baseUrl, meta(doc, "meta[property=og:image:secure_url]", "content")),
        validateAndAbsolutize(baseUrl, meta(doc, "meta[property=og:image:url]", "content")),
        validateAndAbsolutize(baseUrl, meta(doc, "meta[property=og:image]", "content")),
        validateAndAbsolutize(baseUrl, meta(doc, "meta[name=twitter:image:src]", "content")),
        validateAndAbsolutize(baseUrl, meta(doc, "meta[name=twitter:image]", "content")),
        firstOf(slices.imageCandidates())
    );
  }

  /**
   * 내부 다중 공백을 1칸으로, trim. 결과가 비면 null
   */
  public String normalize(String s) {
    if (s == null) {
      return null;
    }
    String v = s.replaceAll("\\s+", " ").trim();
    return v.isEmpty() ? null : v;
  }

  /**
   * 상대/스킴-상대 → 절대 URL 보정 후 http/https만 통과
   */
  public String validateAndAbsolutize(String baseUrl, String value) {
    String abs = absolutize(baseUrl, value);
    return isHttpUrl(abs) ? abs : null;
  }

  /**
   * script 원문(JSON) 한 개를 추출 - 외부 src 스크립트 제외
   */
  private String firstDataByAttrValue(Document doc, String attr, String value) {
    try {
      Elements elements = doc.getElementsByAttributeValue(attr, value);
      for (Element el : elements) {
        if (el.hasAttr("src") && !el.attr("src").isBlank()) {
          continue; // 외부 스크립트 제외
        }
        String raw = el.data();                // script 태그 내부 원문(JSON)
        if (raw == null) {
          continue;
        }
        raw = raw.trim();
        if (raw.isEmpty()) {
          continue;
        }
        return (raw.length() > MAX_JSON_PRIMARY) ? raw.substring(0, MAX_JSON_PRIMARY) : raw;
      }
      return null; // 못 찾으면 null
    } catch (Exception e) {
      return null; // 파싱 실패는 무시(다른 경로 시도)
    }
  }

  /**
   * 이미지 후보 수집: og/twitter 메타 + 본문 <img> (절대경로/스킴 검증 포함)
   */
  private List<String> collectImageCandidates(Document doc, String baseUrl, int maxItems) {
    Set<String> uniq = new LinkedHashSet<>();

    // 메타 이미지(신뢰도 높음)
    pushUrl(uniq,
        validateAndAbsolutize(baseUrl, meta(doc, "meta[property=og:image:secure_url]", "content")));
    pushUrl(uniq,
        validateAndAbsolutize(baseUrl, meta(doc, "meta[property=og:image:url]", "content")));
    pushUrl(uniq, validateAndAbsolutize(baseUrl, meta(doc, "meta[property=og:image]", "content")));
    pushUrl(uniq,
        validateAndAbsolutize(baseUrl, meta(doc, "meta[name=twitter:image:src]", "content")));
    pushUrl(uniq, validateAndAbsolutize(baseUrl, meta(doc, "meta[name=twitter:image]", "content")));

    // 본문 <img> (data-src → src 순으로)
    Elements images = doc.getElementsByTag("img");
    for (Element img : images) {
      String u = firstNonBlank(img.attr("data-src"), img.attr("data-original"), img.attr("src"));
      pushUrl(uniq, validateAndAbsolutize(baseUrl, u));
      if (uniq.size() >= maxItems) {
        break;
      }
    }
    return new ArrayList<>(uniq);
  }

  /**
   * 유효한(확장자/스킴) 이미지 URL만 후보에 추가
   */
  private void pushUrl(Set<String> uniq, String url) {
    if (isLikelyImage(url)) {
      uniq.add(url);
    }
  }

  /**
   * 간단 이미지 URL 판정: http(s) + jpg/png/webp/gif 확장자
   */
  private boolean isLikelyImage(String url) {
    if (!isHttpUrl(url)) {
      return false;
    }
    String lower = url.toLowerCase();
    if (lower.matches(".*\\.(jpg|jpeg|png|webp|gif)(\\?.*)?$")) {
      return true;
    }
    if (lower.contains("format=webp") || lower.contains("format=png") || lower.contains(
        "format=jpg") || lower.contains("format=jpeg")) {
      return true;
    }
    // 잡음 제거
    if (lower.contains("logo") || lower.contains("icon") || lower.contains("sprite")
        || lower.contains("placeholder")) {
      return false;
    }
    return false;
  }

  /**
   * 첫 번째 요소의 텍스트를 공백 정규화하여 반환
   */
  private String firstText(Document doc, String selector) {
    Element el = doc.selectFirst(selector);
    if (el == null) {
      return null;
    }
    String t = el.text();
    if (t == null) {
      return null;
    }
    t = t.replaceAll("\\s+", " ").trim();
    return t.isEmpty() ? null : t;
  }

  /**
   * CSS 셀렉터로 태그 하나를 찾아 attr 값을 반환(비거나 공백뿐이면 null)
   */
  private String meta(Document doc, String selector, String attr) {
    Element el = doc.selectFirst(selector);
    if (el == null) {
      return null;
    }
    String v = el.attr(attr);
    return (v == null || v.isBlank()) ? null : v;
  }

  /**
   * 리스트 첫 요소 또는 null
   */
  private String firstOf(List<String> list) {
    return (list == null || list.isEmpty()) ? null : list.get(0);
  }

  /**
   * 여러 후보 중 비어있지 않은 첫 문자열
   */
  private String firstNonBlank(String... candidates) {
    if (candidates == null) {
      return null;
    }
    for (String c : candidates) {
      if (c != null && !c.isBlank()) {
        return c;
      }
    }
    return null;
  }

  /*** null-safe trim (빈 문자열이면 null)*/
  private String safeTrim(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }


  /**
   * 절대 URL 만들기 (data/blob/file/ftp 차단, //cdn 스킴 보정, 상대경로 resolve)
   */
  private String absolutize(String baseUrl, String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String v = value.trim();
    String lower = v.toLowerCase();
    if (lower.startsWith("data:") || lower.startsWith("blob:")
        || lower.startsWith("file:") || lower.startsWith("ftp:")) {
      return null;
    }
    try {
      URI c = URI.create(v);
      if (c.isAbsolute()) {
        return isHttpUrl(v) ? v : null; // 이미 절대 URL
      }
    } catch (Exception ignore) {
    }
    try {
      URI base = URI.create(baseUrl);
      if (v.startsWith("//")) {                            // 스킴-상대 //cdn...
        String scheme = base.getScheme() == null ? "https" : base.getScheme();
        String full = scheme + ":" + v;
        return isHttpUrl(full) ? full : null;
      }
      String resolved = base.resolve(v).toString();        // 일반 상대경로
      return isHttpUrl(resolved) ? resolved : null;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * http/https만 true
   */
  private static boolean isHttpUrl(String url) {
    if (url == null) {
      return false;
    }
    try {
      String s = URI.create(url).getScheme();
      return "http".equalsIgnoreCase(s) || "https".equalsIgnoreCase(s);
    } catch (Exception e) {
      return false;
    }
  }
}

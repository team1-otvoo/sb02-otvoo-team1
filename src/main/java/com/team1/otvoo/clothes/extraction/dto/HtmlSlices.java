package com.team1.otvoo.clothes.extraction.dto;

import java.util.List;

public record HtmlSlices(
    String baseUrl,
    String title,
    String h1,
    String primaryJson,
    List<String> extraJsons,
    List<String> imageCandidates
) {

}

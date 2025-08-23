package com.team1.otvoo.user.dto;

public record ProfileImageMetaData(
    String objectKey,
    String originalFilename,
    String contentType,
    Long size,
    Integer width,
    Integer height
) {

}

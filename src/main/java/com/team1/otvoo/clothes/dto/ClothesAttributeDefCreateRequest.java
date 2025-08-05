package com.team1.otvoo.clothes.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ClothesAttributeDefCreateRequest(
    @NotBlank(message = "의상 속성 이름은 필수입니다.")
    String name,

    List<@NotBlank(message = "공백은 입력할 수 없습니다.") String> selectableValues
) {

}

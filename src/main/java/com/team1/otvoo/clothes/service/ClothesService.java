package com.team1.otvoo.clothes.service;

import com.team1.otvoo.clothes.dto.ClothesCreateRequest;
import com.team1.otvoo.clothes.dto.ClothesDto;
import com.team1.otvoo.clothes.dto.ClothesDtoCursorResponse;
import com.team1.otvoo.clothes.dto.ClothesSearchCondition;
import com.team1.otvoo.clothes.dto.ClothesUpdateRequest;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface ClothesService {

  ClothesDto create(ClothesCreateRequest request, MultipartFile imageFile);

  ClothesDtoCursorResponse getClothesList(ClothesSearchCondition condition);

  ClothesDto update(UUID clothesId, ClothesUpdateRequest request, MultipartFile imageFile);

  void delete(UUID clothesId);
}

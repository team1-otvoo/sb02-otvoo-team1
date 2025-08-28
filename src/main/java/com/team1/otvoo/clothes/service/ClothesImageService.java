package com.team1.otvoo.clothes.service;

import com.team1.otvoo.clothes.entity.Clothes;
import com.team1.otvoo.clothes.entity.ClothesImage;
import org.springframework.web.multipart.MultipartFile;

public interface ClothesImageService {

  ClothesImage create(Clothes clothes, MultipartFile file);

  void delete(ClothesImage clothesImage);
}
package com.team1.otvoo.clothes.service;

import com.team1.otvoo.clothes.dto.ClothesCreateRequest;
import com.team1.otvoo.clothes.dto.ClothesDto;
import com.team1.otvoo.clothes.dto.ClothesDtoCursorResponse;
import com.team1.otvoo.clothes.dto.ClothesSearchCondition;
import com.team1.otvoo.clothes.dto.ClothesUpdateRequest;
import com.team1.otvoo.clothes.dto.SortBy;
import com.team1.otvoo.clothes.dto.SortDirection;
import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeDto;
import com.team1.otvoo.clothes.entity.Clothes;
import com.team1.otvoo.clothes.entity.ClothesAttributeDefinition;
import com.team1.otvoo.clothes.entity.ClothesAttributeValue;
import com.team1.otvoo.clothes.entity.ClothesImage;
import com.team1.otvoo.clothes.entity.ClothesSelectedValue;
import com.team1.otvoo.clothes.event.ClothesCreatedEvent;
import com.team1.otvoo.clothes.mapper.ClothesMapper;
import com.team1.otvoo.clothes.repository.ClothesAttributeDefRepository;
import com.team1.otvoo.clothes.repository.ClothesAttributeValueRepository;
import com.team1.otvoo.clothes.repository.ClothesImageRepository;
import com.team1.otvoo.clothes.repository.ClothesRepository;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.storage.S3ImageStorage;
import com.team1.otvoo.security.CustomUserDetails;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClothesServiceImpl implements ClothesService {

  private final ClothesRepository clothesRepository;
  private final ClothesAttributeDefRepository clothesAttributeDefRepository;
  private final ClothesAttributeValueRepository clothesAttributeValueRepository;
  private final UserRepository userRepository;
  private final ClothesMapper clothesMapper;
  private final ClothesImageService clothesImageService;
  private final ClothesImageRepository clothesImageRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final S3ImageStorage s3ImageStorage;


  @Override
  @Transactional
  public ClothesDto create(ClothesCreateRequest request, MultipartFile imageFile) {
    List<ClothesAttributeDto> attributes =
        Optional.ofNullable(request.attributes()).orElse(List.of());

    checkDuplicateDefinition(attributes);

    User owner = userRepository.findById(request.ownerId())
        .orElseThrow(() -> new RestException(ErrorCode.USER_NOT_FOUND,
            Map.of("ownerId", request.ownerId())));

    List<ClothesSelectedValue> selectedValues = attributes.stream()
        .map(this::toSelectedValue)
        .toList();

    Clothes clothes = new Clothes(
        owner,
        request.name(),
        request.type(),
        selectedValues
    );
    Clothes saved = clothesRepository.save(clothes);

    // 이미지 처리
    String imageUrl = null;
    if (imageFile != null && !imageFile.isEmpty()) {
      ClothesImage image = clothesImageService.create(saved, imageFile);
      imageUrl = getPresignedUrl(image);
    }

    eventPublisher.publishEvent(new ClothesCreatedEvent(saved, imageUrl));

    return clothesMapper.toDto(saved, imageUrl);
  }

  @Override
  @Transactional(readOnly = true)
  public ClothesDtoCursorResponse getClothesList(ClothesSearchCondition condition) {
    List<Clothes> clothesList = clothesRepository.searchWithCursor(condition);

    boolean hasNext = clothesList.size() > condition.limit();
    List<Clothes> page = hasNext ? clothesList.subList(0, condition.limit()) : clothesList;

    String nextCursor = null;
    UUID nextIdAfter = null;

    if (hasNext) {
      Clothes last = page.get(page.size() - 1);
      nextCursor = last.getCreatedAt().toString();
      nextIdAfter = last.getId();
    }

    long totalCount = clothesRepository.countWithCondition(condition);

    List<UUID> clothesIds = page.stream().map(Clothes::getId).toList();

    Map<UUID, ClothesImage> imageMap = clothesImageRepository.findAllByClothes_IdIn(clothesIds)
        .stream().collect(Collectors.toMap(ci -> ci.getClothes().getId(), ci -> ci));

    List<ClothesDto> data = page.stream()
        .map(clothes -> {
          ClothesImage image = imageMap.get(clothes.getId());
          String imageUrl = getPresignedUrl(image);
          return clothesMapper.toDto(clothes, imageUrl);
        })
        .toList();

    return new ClothesDtoCursorResponse(
        data,
        nextCursor,
        nextIdAfter,
        hasNext,
        totalCount,
        SortBy.CREATED_AT,
        SortDirection.DESCENDING
    );
  }

  @Override
  @Transactional
  public ClothesDto update(CustomUserDetails userDetails, UUID clothesId, ClothesUpdateRequest request, MultipartFile imageFile) {
    Clothes clothes = getClothes(clothesId);

    if (userDetails.getUser().getId() != clothes.getOwner().getId()) {
      throw new RestException(ErrorCode.CLOTHES_UNAUTHORIZED, Map.of("ownerId", clothes.getOwner().getId(), "userId", userDetails.getUser().getId()));
    }

    clothes.updateName(request.name());
    clothes.updateType(request.type());

    if (request.attributes() != null) {
      checkDuplicateDefinition(request.attributes());

      Map<UUID, UUID> currentMap = clothes.getSelectedValues().stream().
          collect(Collectors.toMap(
              sv -> sv.getDefinition().getId(),
              sv -> sv.getValue().getId()
          ));
      Map<UUID, UUID> requestMap = new HashMap<>();
      List<ClothesSelectedValue> newSelectedValues = new ArrayList<>();
      for (ClothesAttributeDto attr : request.attributes()) {
        ClothesSelectedValue selectedValue = toSelectedValue(attr);
        requestMap.put(selectedValue.getDefinition().getId(), selectedValue.getValue().getId());
        newSelectedValues.add(selectedValue);
      }

      if (!currentMap.equals(requestMap)) {
        clothes.clearSelectedValues();
        clothesRepository.flush();
        clothes.addSelectedValues(newSelectedValues);
      }
    }
    Clothes saved = clothesRepository.save(clothes);
    // 이미지 처리
    String imageUrl = null;
    if (imageFile != null && !imageFile.isEmpty()) {
      ClothesImage image = clothesImageService.create(saved, imageFile);
      imageUrl = getPresignedUrl(image);
    } else {
      imageUrl = clothesImageRepository.findByClothes_Id(clothes.getId())
          .map(this::getPresignedUrl)
          .orElse(null);
    }
    return clothesMapper.toDto(saved, imageUrl);
  }

  @Override
  @Transactional
  public void delete(CustomUserDetails userDetails, UUID clothesId) {
    Clothes clothes = getClothes(clothesId);

    if (userDetails.getUser().getId() != clothes.getOwner().getId()) {
      throw new RestException(ErrorCode.CLOTHES_UNAUTHORIZED, Map.of("ownerId", clothes.getOwner().getId(), "userId", userDetails.getUser().getId()));
    }

    Optional<ClothesImage> image = clothesImageRepository.findByClothes_Id(clothes.getId());
    image.ifPresent(clothesImageService::delete);
    clothesRepository.delete(clothes);
  }

  private Clothes getClothes(UUID clothesId) {
    return clothesRepository.findById(clothesId)
        .orElseThrow(
            () -> new RestException(ErrorCode.CLOTHES_NOT_FOUND, Map.of("clothesId", clothesId)));
  }

  private ClothesSelectedValue toSelectedValue(ClothesAttributeDto attr) {
    ClothesAttributeDefinition def = getDefinition(attr.definitionId());
    ClothesAttributeValue val = getAttributeValue(def.getId(), attr.value());
    return new ClothesSelectedValue(def, val);
  }

  private ClothesAttributeDefinition getDefinition(UUID definitionId) {
    return clothesAttributeDefRepository.findById(definitionId)
        .orElseThrow(() -> new RestException(ErrorCode.ATTRIBUTE_DEFINITION_NOT_FOUND,
            Map.of("definitionId", definitionId)));
  }

  private ClothesAttributeValue getAttributeValue(UUID definitionId, String value) {
    return clothesAttributeValueRepository.findByDefinitionIdAndValue(definitionId, value)
        .orElseThrow(() -> new RestException(ErrorCode.ATTRIBUTE_VALUE_NOT_FOUND,
            Map.of("definitionId", definitionId, "value", value)));
  }

  private void checkDuplicateDefinition(List<ClothesAttributeDto> attributes) {
    if (attributes == null || attributes.isEmpty()) {
      return;
    }
    Set<UUID> unique = new HashSet<>();
    for (ClothesAttributeDto attr : attributes) {
      if (!unique.add(attr.definitionId())) {
        throw new RestException(
            ErrorCode.ATTRIBUTE_DEFINITION_DUPLICATE,
            Map.of("definitionId", attr.definitionId()));
      }
    }
  }

  private String getPresignedUrl(ClothesImage image) {
    if (image == null || image.getImageKey() == null) {
      return null;
    }
    try {
      return s3ImageStorage.getPresignedUrl(image.getImageKey(), image.getContentType());
    } catch (Exception e) {
      log.warn("presigned URL 생성 실패: key={}, msg={}",
          image.getImageKey(), e.getMessage(), e);
      return null;
    }
  }
}

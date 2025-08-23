package com.team1.otvoo.user.mapper;

import com.team1.otvoo.user.dto.ProfileImageMetaData;
import com.team1.otvoo.user.entity.ProfileImage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProfileImageMapper {

  ProfileImageMetaData profileImageToProfileImageMetaData(ProfileImage profileImage);

}

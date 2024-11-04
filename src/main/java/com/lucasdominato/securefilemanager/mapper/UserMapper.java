package com.lucasdominato.securefilemanager.mapper;

import com.lucasdominato.securefilemanager.data.entity.User;
import com.lucasdominato.securefilemanager.dto.UserDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {
        DateMapper.class
})
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "files", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(source = "dateOfBirth", target = "dateOfBirth")
    User mapEntity(UserDTO userDTO);
}
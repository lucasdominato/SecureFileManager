package com.lucasdominato.securefilemanager.mapper;

import com.lucasdominato.securefilemanager.data.entity.File;
import com.lucasdominato.securefilemanager.dto.command.UpdateFileCommand;
import com.lucasdominato.securefilemanager.dto.response.FileResponseDTO;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface FileMapper {

    FileResponseDTO fileToFileDto(File file);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFileFromCommand(UpdateFileCommand updateFileCommand, @MappingTarget File file);
}
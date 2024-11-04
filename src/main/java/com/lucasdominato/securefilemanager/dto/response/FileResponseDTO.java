package com.lucasdominato.securefilemanager.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FileResponseDTO {

    private Long id;
    private String name;
    private String description;
    private String contentType;
    private Long fileSize;
}
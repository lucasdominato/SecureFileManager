package com.lucasdominato.securefilemanager.dto.command;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateFileCommand {

    private String name;
    private String description;
    private String contentType;
    private Long fileSize;

    public UpdateFileCommand(String updatedDescription) {
        this.description = updatedDescription;
    }
}

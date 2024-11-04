package com.lucasdominato.securefilemanager.dto.command;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateFileCommand {

    private String name;
    private String description;
    private String contentType;
    private long fileSize;
}
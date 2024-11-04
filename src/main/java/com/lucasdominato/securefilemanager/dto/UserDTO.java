package com.lucasdominato.securefilemanager.dto;

import lombok.Data;

@Data
public class UserDTO {
    private String username;
    private String name;
    private String email;
    private String dateOfBirth;
}

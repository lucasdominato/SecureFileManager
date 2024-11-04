package com.lucasdominato.securefilemanager.mapper;

import com.lucasdominato.securefilemanager.dto.UserDTO;
import com.lucasdominato.securefilemanager.security.CustomUserDetails;
import org.springframework.security.core.Authentication;


public class AuthenticationMapper {

    public static UserDTO toUserDTO(Authentication authentication) {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername(authentication.getName());

        if (authentication.getPrincipal() != null) {
            CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
            userDTO.setName(principal.getName());
            userDTO.setEmail(principal.getEmail());
            userDTO.setDateOfBirth(principal.getDateOfBirth());
        }

        return userDTO;
    }
}
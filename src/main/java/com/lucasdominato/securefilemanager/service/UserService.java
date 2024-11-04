package com.lucasdominato.securefilemanager.service;

import com.lucasdominato.securefilemanager.data.entity.User;
import com.lucasdominato.securefilemanager.data.repository.UserRepository;
import com.lucasdominato.securefilemanager.dto.UserDTO;
import com.lucasdominato.securefilemanager.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(final UserRepository userRepository,
                       final UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public User getOrCreateUser(final UserDTO userDto) {
        Optional<User> existingUser = userRepository.findByUsername(userDto.getUsername());

        if (existingUser.isPresent()) {
            return existingUser.get();
        } else {
            User newUser = userMapper.mapEntity(userDto);
            return userRepository.save(newUser);
        }
    }
}
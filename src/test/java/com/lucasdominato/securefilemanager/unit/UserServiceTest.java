package com.lucasdominato.securefilemanager.unit;

import com.lucasdominato.securefilemanager.data.entity.User;
import com.lucasdominato.securefilemanager.data.repository.UserRepository;
import com.lucasdominato.securefilemanager.dto.UserDTO;
import com.lucasdominato.securefilemanager.mapper.UserMapper;
import com.lucasdominato.securefilemanager.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserDTO userDto;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testUser");

        userDto = new UserDTO();
        userDto.setUsername("testUser");
    }

    @Test
    void getOrCreateUser_shouldReturnExistingUser_whenUserExists() {
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(user));

        User result = userService.getOrCreateUser(userDto);

        assertNotNull(result);
        assertEquals("testUser", result.getUsername());
        verify(userRepository, times(1)).findByUsername("testUser");
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(userMapper);
    }

    @Test
    void getOrCreateUser_shouldCreateAndReturnNewUser_whenUserDoesNotExist() {
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.empty());
        when(userMapper.mapEntity(userDto)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.getOrCreateUser(userDto);

        assertNotNull(result);
        assertEquals("testUser", result.getUsername());
        verify(userRepository, times(1)).findByUsername("testUser");
        verify(userMapper, times(1)).mapEntity(userDto);
        verify(userRepository, times(1)).save(user);
    }
}
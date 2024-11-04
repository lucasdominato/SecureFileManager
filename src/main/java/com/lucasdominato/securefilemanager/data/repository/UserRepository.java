package com.lucasdominato.securefilemanager.data.repository;

import com.lucasdominato.securefilemanager.data.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
package com.lucasdominato.securefilemanager.data.repository;

import com.lucasdominato.securefilemanager.data.entity.File;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FileRepository extends JpaRepository<File, Long> {
    Page<File> findAllByUserUsername(String username, Pageable pageable);
    Optional<File> findByIdAndUserUsername(Long id, String username);
}

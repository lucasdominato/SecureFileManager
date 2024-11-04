package com.lucasdominato.securefilemanager.data.repository;

import com.lucasdominato.securefilemanager.data.entity.FileContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileContentRepository extends JpaRepository<FileContent, Long> {
    void deleteByFileId(Long id);
}
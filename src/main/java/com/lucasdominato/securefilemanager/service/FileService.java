package com.lucasdominato.securefilemanager.service;

import com.lucasdominato.securefilemanager.data.entity.File;
import com.lucasdominato.securefilemanager.data.entity.User;
import com.lucasdominato.securefilemanager.data.repository.FileContentRepository;
import com.lucasdominato.securefilemanager.data.repository.FileJdbcRepository;
import com.lucasdominato.securefilemanager.data.repository.FileRepository;
import com.lucasdominato.securefilemanager.dto.UserDTO;
import com.lucasdominato.securefilemanager.dto.command.CreateFileCommand;
import com.lucasdominato.securefilemanager.dto.command.UpdateFileCommand;
import com.lucasdominato.securefilemanager.dto.response.FileResponseDTO;
import com.lucasdominato.securefilemanager.exception.FileNotFoundException;
import com.lucasdominato.securefilemanager.mapper.FileMapper;
import com.lucasdominato.securefilemanager.security.EncryptionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.sql.SQLException;

@Service
public class FileService {

    private final FileContentRepository fileContentRepository;
    private final FileJdbcRepository fileJdbcRepository;
    private final EncryptionService encryptionService;
    private final FileRepository fileRepository;
    private final UserService userService;
    private final FileMapper fileMapper;

    public FileService(final FileContentRepository fileContentRepository,
                       final FileJdbcRepository fileJdbcRepository,
                       final EncryptionService encryptionService,
                       final FileRepository fileRepository,
                       final UserService userService,
                       final FileMapper fileMapper) {
        this.fileContentRepository = fileContentRepository;
        this.fileJdbcRepository = fileJdbcRepository;
        this.encryptionService = encryptionService;
        this.fileRepository = fileRepository;
        this.userService = userService;
        this.fileMapper = fileMapper;
    }

    @Transactional(readOnly = true)
    public Page<FileResponseDTO> getFilesByUsername(final String username, final Pageable pageable) {
        return fileRepository.findAllByUserUsername(username, pageable)
                .map(fileMapper::fileToFileDto);
    }

    @Transactional(readOnly = true)
    public FileResponseDTO getFileByIdAndUsername(final Long id,
                                                  final String username) {
        return fileRepository.findByIdAndUserUsername(id, username)
                .map(fileMapper::fileToFileDto)
                .orElseThrow(() -> new FileNotFoundException("File not found"));
    }

    @Transactional
    public FileResponseDTO createFile(final CreateFileCommand fileCommand,
                                      final InputStream inputStream,
                                      final UserDTO userDto) throws IOException, GeneralSecurityException, SQLException {
        User user = userService.getOrCreateUser(userDto);

        File fileEntity = new File();
        fileEntity.setName(fileCommand.getName());
        fileEntity.setDescription(fileCommand.getDescription());
        fileEntity.setUser(user);
        fileEntity.setContentType(fileCommand.getContentType());
        fileEntity.setFileSize(fileCommand.getFileSize());
        fileRepository.save(fileEntity);

        try (InputStream encryptedInputStream = encryptionService.encryptStream(inputStream)) {
            fileJdbcRepository.upsertFileContent(fileEntity.getId(), encryptedInputStream);
        }

        return fileMapper.fileToFileDto(fileEntity);
    }

    @Transactional(readOnly = true)
    public void downloadFile(final Long fileId,
                             final OutputStream outputStream) throws IOException, SQLException, GeneralSecurityException {
        try (InputStream inputStream = fileJdbcRepository.getFileContentStreamByFileId(fileId)) {
            encryptionService.decryptStream(inputStream, outputStream);
        }
    }

    @Transactional
    public FileResponseDTO updateFileWithContent(final Long id,
                                                 final UpdateFileCommand updateFileCommand,
                                                 final InputStream inputStream,
                                                 final String username) {
        File file = fileRepository.findByIdAndUserUsername(id, username)
                .orElseThrow(() -> new FileNotFoundException("File not found"));

        fileMapper.updateFileFromCommand(updateFileCommand, file);

        File savedFile = fileRepository.save(file);

        try (InputStream encryptedInputStream = encryptionService.encryptStream(inputStream)) {
            fileJdbcRepository.upsertFileContent(savedFile.getId(), encryptedInputStream);
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }

        return fileMapper.fileToFileDto(savedFile);
    }

    @Transactional
    public FileResponseDTO updateFile(final Long id,
                                      final UpdateFileCommand updateFileCommand,
                                      final String username) {
        File file = fileRepository.findByIdAndUserUsername(id, username)
                .orElseThrow(() -> new FileNotFoundException("File not found"));

        fileMapper.updateFileFromCommand(updateFileCommand, file);

        return fileMapper.fileToFileDto(fileRepository.save(file));
    }

    @Transactional
    public void deleteFile(final Long id, final String username) {
        File file = fileRepository.findByIdAndUserUsername(id, username)
                .orElseThrow(() -> new FileNotFoundException("File not found"));

        fileContentRepository.deleteByFileId(file.getId());
        fileRepository.delete(file);
    }
}
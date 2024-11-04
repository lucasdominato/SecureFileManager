package com.lucasdominato.securefilemanager.unit;

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
import com.lucasdominato.securefilemanager.service.FileService;
import com.lucasdominato.securefilemanager.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.io.*;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FileServiceTest {

    @Mock
    private FileContentRepository fileContentRepository;

    @Mock
    private FileJdbcRepository fileJdbcRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private UserService userService;

    @Mock
    private FileMapper fileMapper;

    @InjectMocks
    private FileService fileService;

    private UserDTO userDTO;
    private File fileEntity;
    private FileResponseDTO fileResponseDTO;
    private CreateFileCommand createFileCommand;
    private UpdateFileCommand updateFileCommand;

    @BeforeEach
    void setup() {
        userDTO = new UserDTO();
        userDTO.setUsername("username");
        fileEntity = new File();
        fileEntity.setId(1L);
        fileEntity.setName("testfile");
        fileEntity.setContentType("text/plain");
        fileEntity.setFileSize(100L);

        fileResponseDTO = new FileResponseDTO();
        createFileCommand = new CreateFileCommand("testfile", "description", "text/plain", 100L);
        updateFileCommand = new UpdateFileCommand("updatedDescription");
    }

    @Test
    void testGetFilesByUsername() {
        Page<File> page = new PageImpl<>(Collections.singletonList(fileEntity));
        PageRequest pageable = PageRequest.of(0, 10);
        when(fileRepository.findAllByUserUsername(anyString(), eq(pageable))).thenReturn(page);
        when(fileMapper.fileToFileDto(any())).thenReturn(fileResponseDTO);

        Page<FileResponseDTO> result = fileService.getFilesByUsername("username", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(fileRepository).findAllByUserUsername("username", pageable);
    }

    @Test
    void testGetFileByIdAndUsername_FileFound() {
        when(fileRepository.findByIdAndUserUsername(anyLong(), anyString())).thenReturn(Optional.of(fileEntity));
        when(fileMapper.fileToFileDto(any())).thenReturn(fileResponseDTO);

        FileResponseDTO result = fileService.getFileByIdAndUsername(1L, "username");

        assertNotNull(result);
        verify(fileRepository).findByIdAndUserUsername(1L, "username");
    }

    @Test
    void testGetFileByIdAndUsername_FileNotFound() {
        when(fileRepository.findByIdAndUserUsername(anyLong(), anyString())).thenReturn(Optional.empty());

        assertThrows(FileNotFoundException.class, () -> fileService.getFileByIdAndUsername(1L, "username"));
        verify(fileRepository).findByIdAndUserUsername(1L, "username");
    }

    @Test
    void testCreateFile() throws IOException, GeneralSecurityException, SQLException {
        User user = new User();
        when(userService.getOrCreateUser(any())).thenReturn(user);

        doAnswer(invocation -> {
            File file = invocation.getArgument(0);
            file.setId(1L);
            return file;
        }).when(fileRepository).save(any(File.class));

        when(fileMapper.fileToFileDto(any())).thenReturn(fileResponseDTO);

        InputStream inputStream = new ByteArrayInputStream(new byte[0]);
        InputStream encryptedInputStream = new ByteArrayInputStream(new byte[0]);
        when(encryptionService.encryptStream(any())).thenReturn(encryptedInputStream);

        FileResponseDTO result = fileService.createFile(createFileCommand, inputStream, userDTO);

        assertNotNull(result);

        verify(fileRepository).save(any(File.class));
        verify(fileJdbcRepository).upsertFileContent(eq(1L), eq(encryptedInputStream));
    }

    @Test
    void testDownloadFile() throws IOException, GeneralSecurityException, SQLException {
        InputStream encryptedInputStream = new ByteArrayInputStream(new byte[0]);
        when(fileJdbcRepository.getFileContentStreamByFileId(anyLong())).thenReturn(encryptedInputStream);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        fileService.downloadFile(1L, outputStream);

        verify(encryptionService).decryptStream(eq(encryptedInputStream), eq(outputStream));
    }

    @Test
    void testUpdateFileWithContent() throws IOException, GeneralSecurityException {
        when(fileRepository.findByIdAndUserUsername(anyLong(), anyString())).thenReturn(Optional.of(fileEntity));
        when(fileRepository.save(any(File.class))).thenReturn(fileEntity);
        when(fileMapper.fileToFileDto(any())).thenReturn(fileResponseDTO);

        InputStream encryptedInputStream = new ByteArrayInputStream(new byte[0]);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(new ByteArrayInputStream(new byte[0]));
        when(encryptionService.encryptStream(any())).thenReturn(encryptedInputStream);

        FileResponseDTO result = fileService.updateFileWithContent(1L, updateFileCommand, bufferedInputStream, "username");

        assertNotNull(result);
        verify(fileRepository).save(fileEntity);
        verify(fileJdbcRepository).upsertFileContent(anyLong(), eq(encryptedInputStream));
    }

    @Test
    void testUpdateFile() {
        when(fileRepository.findByIdAndUserUsername(anyLong(), anyString())).thenReturn(Optional.of(fileEntity));
        when(fileMapper.fileToFileDto(any())).thenReturn(fileResponseDTO);

        FileResponseDTO result = fileService.updateFile(1L, updateFileCommand, "username");

        assertNotNull(result);
        verify(fileRepository).save(fileEntity);
    }

    @Test
    void testDeleteFile() {
        when(fileRepository.findByIdAndUserUsername(anyLong(), anyString())).thenReturn(Optional.of(fileEntity));

        fileService.deleteFile(1L, "username");

        verify(fileContentRepository).deleteByFileId(1L);
        verify(fileRepository).delete(fileEntity);
    }

    @Test
    void testDeleteFile_FileNotFound() {
        when(fileRepository.findByIdAndUserUsername(anyLong(), anyString())).thenReturn(Optional.empty());

        assertThrows(FileNotFoundException.class, () -> fileService.deleteFile(1L, "username"));
        verify(fileRepository).findByIdAndUserUsername(1L, "username");
    }
}
package com.lucasdominato.securefilemanager.rest;

import com.lucasdominato.securefilemanager.dto.command.CreateFileCommand;
import com.lucasdominato.securefilemanager.dto.command.UpdateFileCommand;
import com.lucasdominato.securefilemanager.dto.response.FileResponseDTO;
import com.lucasdominato.securefilemanager.exception.FileProcessingException;
import com.lucasdominato.securefilemanager.mapper.AuthenticationMapper;
import com.lucasdominato.securefilemanager.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.Objects;

@RestController
@RequestMapping("/api/files")
@Tag(name = "File API", description = "API for file management")
public class FileController {

    public static final int UPLOAD_FILE_BUFFER_SIZE = 8192;

    private final FileService fileService;

    public FileController(final FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping
    @Operation(summary = "Get all files",
            description = "Returns a paginated list of files all files",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Files retrieved successfully",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = FileResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request", content = @Content()),
                    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content()),
                    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
            },
            tags = {"File API"})
    public Page<FileResponseDTO> getUserFiles(Authentication authentication, @ParameterObject @PageableDefault final Pageable pageable) {
        return fileService.getFilesByUsername(authentication.getName(), pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get file by id",
            description = "Returns a file by id",
            responses = {
                    @ApiResponse(responseCode = "200", description = "File retrieved successfully",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = FileResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content()),
                    @ApiResponse(responseCode = "404", description = "File not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            },
            tags = {"File API"})
    public ResponseEntity<FileResponseDTO> getFile(Authentication authentication, @NotNull @PathVariable Long id) {
        return ResponseEntity.ok(fileService.getFileByIdAndUsername(id, authentication.getName()));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download file content by id",
            description = "Downloads the file content for the specified file id. The content is streamed securely to the client.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "File retrieved successfully"),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content()),
                    @ApiResponse(responseCode = "404", description = "File not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            },
            tags = {"File API"})
    public void downloadFile(Authentication authentication,
                             @NotNull @PathVariable Long id,
                             HttpServletResponse response) throws IOException {
        FileResponseDTO fileInfo = fileService.getFileByIdAndUsername(id, authentication.getName());

        response.setContentType(fileInfo.getContentType());
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileInfo.getName() + "\"");

        try (OutputStream outputStream = response.getOutputStream()) {
            fileService.downloadFile(id, outputStream);
        } catch (SQLException | GeneralSecurityException e) {
            throw new FileProcessingException("Failed to process file content");
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create a file",
            description = "Creates a file with the specified metadata and file content. The file is securely stored in the database, and the content is encrypted during the process.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "File created successfully",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CreateFileCommand.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content()),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            },
            tags = {"File API"})
    public ResponseEntity<FileResponseDTO> createFile(Authentication authentication,
                                                      @RequestParam(required = false) String description,
                                                      @RequestParam MultipartFile file) {
        CreateFileCommand createFileDTO = new CreateFileCommand();
        createFileDTO.setName(file.getOriginalFilename());
        createFileDTO.setDescription(description);
        createFileDTO.setContentType(file.getContentType());
        createFileDTO.setFileSize(file.getSize());

        try (BufferedInputStream bufferedInput = new BufferedInputStream(file.getInputStream(), UPLOAD_FILE_BUFFER_SIZE)) {
            FileResponseDTO response = fileService.createFile(
                    createFileDTO,
                    bufferedInput,
                    AuthenticationMapper.toUserDTO(authentication)
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IOException | GeneralSecurityException | SQLException e) {
            throw new FileProcessingException("Failed to process file stream");
        }
    }

    @PatchMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Partial update a file by id",
            description = "Partially update a file by id",
            responses = {
                    @ApiResponse(responseCode = "200", description = "File updated successfully",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UpdateFileCommand.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content()),
                    @ApiResponse(responseCode = "404", description = "File not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            },
            tags = {"File API"})
    public ResponseEntity<FileResponseDTO> updateFile(Authentication authentication,
                                                      @NotNull @PathVariable Long id,
                                                      @RequestParam(required = false) String description,
                                                      @RequestParam(required = false) MultipartFile file) {
        if (StringUtils.isEmpty(description) && Objects.isNull(file)) {
            throw new IllegalArgumentException("A file or description must be provided for update");
        }

        UpdateFileCommand updateFileCommand = new UpdateFileCommand();
        updateFileCommand.setDescription(description);

        if (file != null){
            updateFileCommand.setName(file.getOriginalFilename());
            updateFileCommand.setContentType(file.getContentType());
            updateFileCommand.setFileSize(file.getSize());

            try (BufferedInputStream bufferedInput = new BufferedInputStream(file.getInputStream(), UPLOAD_FILE_BUFFER_SIZE)) {
                FileResponseDTO response = fileService.updateFileWithContent(
                        id,
                        updateFileCommand,
                        bufferedInput,
                        authentication.getName()
                );

                return ResponseEntity.ok(response);
            } catch (IOException e) {
                throw new FileProcessingException("Failed to process file stream");
            }
        }

        return ResponseEntity.ok(fileService.updateFile(id, updateFileCommand, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a file by id",
            description = "Deletes a file by id",
            responses = {
                    @ApiResponse(responseCode = "204", description = "File deleted successfully"),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content()),
                    @ApiResponse(responseCode = "404", description = "File not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            },
            tags = {"File API"})
    public ResponseEntity<Void> deleteFile(Authentication authentication, @NotNull @PathVariable Long id) {
        fileService.deleteFile(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
package com.lucasdominato.securefilemanager.integration;

import com.lucasdominato.securefilemanager.AbstractIntegrationTest;
import com.lucasdominato.securefilemanager.data.repository.FileContentRepository;
import com.lucasdominato.securefilemanager.data.repository.FileRepository;
import com.lucasdominato.securefilemanager.data.repository.UserRepository;
import com.lucasdominato.securefilemanager.dto.response.FileResponseDTO;
import com.lucasdominato.securefilemanager.security.JwtUtil;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FileIntegrationTest extends AbstractIntegrationTest {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private FileContentRepository fileContentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private String token;

    private final ClassPathResource dummyFile = new ClassPathResource("dummy.pdf");

    String generateValidToken(final String username) {
        return jwtUtil.generateToken(username, "User Foo", "userfoo@gmail.com", "01-01-1990");
    }

    void generateValidToken() {
        token = jwtUtil.generateToken("testUser", "User Foo", "userfoo@gmail.com", "01-01-1990");
    }

    void generateExpiredToken() {
        final Date issuedAt = new Date(System.currentTimeMillis() - 1800000); // 10 minutes ago
        final Date expiration = new Date(System.currentTimeMillis() - 300000); // 5 minutes ago

        token = jwtUtil.generateToken("testUser", "User Foo", "userfoo@gmail.com", "01-01-1990", issuedAt, expiration);
    }

    void generateInvalidToken() {
        final Date issuedAt = new Date(System.currentTimeMillis());
        final Date expiration = new Date(System.currentTimeMillis() + 300000); // 5 minutes from now

        SecretKey wrongSecretKey = new SecretKeySpec("a-wrong-long-secret-key-for-testing-purposes".getBytes(), "HmacSHA256");
        token = jwtUtil.generateToken("testUser", "User Foo", "userfoo@gmail.com", "01-01-1990", issuedAt, expiration, wrongSecretKey);
    }

    @BeforeEach
    void beforeEach() {
        fileContentRepository.deleteAll();
        fileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldSearchAllFilesWithNoPagingSuccessfully() throws Exception {
        generateValidToken();

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/files")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    void shouldFindAFileByIdSuccessfully() throws Exception {
        generateValidToken();

        FileResponseDTO file = createFile("File 1", "Description 1");

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/files/" + file.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(Matchers.equalTo(file.getId().intValue())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value(Matchers.equalTo("File 1")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value(Matchers.equalTo("Description 1")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.contentType").value(Matchers.equalTo(MediaType.APPLICATION_PDF_VALUE)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.fileSize").value(Matchers.equalTo(dummyFile.getContentAsByteArray().length)))
                .andReturn();
    }

    @Test
    void shouldFailToFindAFileByIdDueFileNotFoundNonOwnerUser() throws Exception {
        generateValidToken();
        FileResponseDTO file = createFile("File 1", "Description 1");

        String anotherUserToken = generateValidToken("anotherUser");

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/files/" + file.getId())
                        .header("Authorization", "Bearer " + anotherUserToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(Matchers.equalTo("File not found")))
                .andReturn();
    }

    @Test
    void shouldFailToFindAFileByIdDueFileNotFound() throws Exception {
        generateValidToken();

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/files/" + Integer.MAX_VALUE)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(Matchers.equalTo("File not found")))
                .andReturn();
    }

    @Test
    void shouldFailToFindAFileByIdDueInvalidParameter() throws Exception {
        generateValidToken();

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/files/invalid")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(Matchers.startsWith("Invalid value for parameter 'id': 'invalid' is not a valid 'Long'")))
                .andReturn();
    }

    @Test
    void shouldSearchAllFilesPagedSuccessfully() throws Exception {
        generateValidToken();

        createFile("File 1", "Description 1");
        createFile("File 2", "Description 2");
        FileResponseDTO file3 = createFile("File 3", "Description 3");

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/files?page=0&size=1&sort=name,desc")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].id").value(Matchers.equalTo(file3.getId().intValue())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.page.number").value(Matchers.equalTo(0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.page.size").value(Matchers.equalTo(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.page.totalElements").value(Matchers.equalTo(3)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.page.totalPages").value(Matchers.equalTo(3)))
                .andReturn();
    }

    @Test
    void shouldSearchAllFilesWithNoPagingNoTokenError() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/api/files")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(Matchers.equalTo("JWT token is missing")))
                .andReturn();
    }

    @Test
    void shouldSearchAllFilesWithNoPagingExpiredTokenError() throws Exception {
        generateExpiredToken();

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/files")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(Matchers.equalTo("Token has expired")))
                .andReturn();
    }

    @Test
    void shouldSearchAllFilesWithNoPagingInvalidTokenError() throws Exception {
        generateInvalidToken();

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/files")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(Matchers.equalTo("Invalid JWT token")))
                .andReturn();
    }

    @Test
    void shouldCreateAFileSuccessfully() throws Exception {
        generateValidToken();

        MockMultipartFile mockedFile = new MockMultipartFile("file", dummyFile.getFilename(), MediaType.APPLICATION_PDF_VALUE, dummyFile.getInputStream());

        mvc.perform(MockMvcRequestBuilders
                        .multipart("/api/files")
                        .file(mockedFile)
                        .param("description", "Description 1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(Matchers.notNullValue()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value(Matchers.equalTo("dummy.pdf")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value(Matchers.equalTo("Description 1")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.contentType").value(Matchers.equalTo(MediaType.APPLICATION_PDF_VALUE)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.fileSize").value(Matchers.equalTo(dummyFile.getContentAsByteArray().length)))
                .andReturn();
    }

    @Test
    void shouldFailToCreateAFileWithInvalidData() throws Exception {
        generateValidToken();

        mvc.perform(MockMvcRequestBuilders
                        .multipart("/api/files")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(Matchers.equalTo("Required part 'file' is not present.")))
                .andReturn();
    }

    @Test
    void shouldPartialUpdateAFileDescriptionSuccessfully() throws Exception {
        generateValidToken();
        FileResponseDTO file = createFile("File 1", "Description 1");

        mvc.perform(MockMvcRequestBuilders
                        .multipart("/api/files/" + file.getId())
                        .param("description", "New file description")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(Matchers.equalTo(file.getId().intValue())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value(Matchers.equalTo("File 1")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value(Matchers.equalTo("New file description")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.contentType").value(Matchers.equalTo(MediaType.APPLICATION_PDF_VALUE)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.fileSize").value(Matchers.equalTo(file.getFileSize().intValue())))
                .andReturn();
    }

    @Test
    void shouldPartialUpdateAFileContentSuccessfully() throws Exception {
        generateValidToken();
        FileResponseDTO file = createFile("dummy1.pdf", "Description 1");

        ClassPathResource dummyFile2 = new ClassPathResource("dummy2.pdf");
        MockMultipartFile mockedFile = new MockMultipartFile("file", "dummy2.pdf", MediaType.APPLICATION_PDF_VALUE, dummyFile2.getInputStream());

        mvc.perform(MockMvcRequestBuilders
                        .multipart("/api/files/" + file.getId())
                        .file(mockedFile)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(Matchers.equalTo(file.getId().intValue())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value(Matchers.equalTo("dummy2.pdf")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value(Matchers.equalTo("Description 1")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.contentType").value(Matchers.equalTo(MediaType.APPLICATION_PDF_VALUE)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.fileSize").value(Matchers.equalTo(dummyFile2.getContentAsByteArray().length)))
                .andReturn();
    }

    @Test
    void shouldPartialUpdateAFileContentAndDescriptionSuccessfully() throws Exception {
        generateValidToken();
        FileResponseDTO file = createFile("File 1", "Description 1");

        ClassPathResource dummyFile2 = new ClassPathResource("dummy2.pdf");
        MockMultipartFile mockedFile = new MockMultipartFile("file", "dummy2.pdf", MediaType.APPLICATION_PDF_VALUE, dummyFile2.getInputStream());

        mvc.perform(MockMvcRequestBuilders
                        .multipart("/api/files/" + file.getId())
                        .file(mockedFile)
                        .param("description", "New file description")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(Matchers.equalTo(file.getId().intValue())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value(Matchers.equalTo("dummy2.pdf")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value(Matchers.equalTo("New file description")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.contentType").value(Matchers.equalTo(MediaType.APPLICATION_PDF_VALUE)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.fileSize").value(Matchers.equalTo(dummyFile2.getContentAsByteArray().length)))
                .andReturn();
    }

    @Test
    void shouldFailToPartialUpdateAFileNameByIdDueFileNotFoundNonOwnerUser() throws Exception {
        generateValidToken();
        FileResponseDTO file = createFile("File 1", "Description 1");

        ClassPathResource dummyFile2 = new ClassPathResource("dummy2.pdf");
        MockMultipartFile mockedFile = new MockMultipartFile("file", "dummy2.pdf", MediaType.APPLICATION_PDF_VALUE, dummyFile2.getInputStream());

        String anotherUserToken = generateValidToken("anotherUser");

        mvc.perform(MockMvcRequestBuilders
                        .multipart("/api/files/" + file.getId())
                        .file(mockedFile)
                        .param("description", "New file description")
                        .header("Authorization", "Bearer " + anotherUserToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andExpect(status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(Matchers.equalTo("File not found")))
                .andReturn();
    }

    @Test
    void shouldFailToPartialUpdateAFileDueMissingFields() throws Exception {
        generateValidToken();
        FileResponseDTO file = createFile("File 1", "Description 1");

        String anotherUserToken = generateValidToken("anotherUser");

        mvc.perform(MockMvcRequestBuilders
                        .multipart("/api/files/" + file.getId())
                        .header("Authorization", "Bearer " + anotherUserToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andExpect(status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(Matchers.equalTo("A file or description must be provided for update")))
                .andReturn();
    }

    @Test
    void shouldDeleteAFileByIdSuccessfully() throws Exception {
        generateValidToken();

        FileResponseDTO file = createFile("File 1", "Description 1");

        mvc.perform(MockMvcRequestBuilders
                        .delete("/api/files/" + file.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent())
                .andReturn();
    }

    @Test
    void shouldFailToDeleteAFileByIdDueFileNotFound() throws Exception {
        generateValidToken();

        mvc.perform(MockMvcRequestBuilders
                        .delete("/api/files/" + Integer.MAX_VALUE)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();
    }

    @Test
    void shouldFailToDeleteAFileByIdDueFileNotFoundNonOwnerUser() throws Exception {
        generateValidToken();

        FileResponseDTO file = createFile("File 1", "Description 1");

        String anotherUserToken = generateValidToken("anotherUser");

        mvc.perform(MockMvcRequestBuilders
                        .delete("/api/files/" + file.getId())
                        .header("Authorization", "Bearer " + anotherUserToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(Matchers.equalTo("File not found")))
                .andReturn();
    }

    @Test
    void shouldDownloadFileSuccessfully() throws Exception {
        generateValidToken();

        FileResponseDTO file = createFile("File 1", "Description 1");

        MvcResult result = mvc.perform(MockMvcRequestBuilders
                        .get("/api/files/" + file.getId() + "/download")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals(MediaType.APPLICATION_PDF_VALUE, result.getResponse().getContentType());
        assertEquals("attachment; filename=\"" + file.getName() + "\"", result.getResponse().getHeader("Content-Disposition"));

        byte[] downloadedContent = result.getResponse().getContentAsByteArray();
        byte[] expectedContent = dummyFile.getInputStream().readAllBytes();
        assertArrayEquals(expectedContent, downloadedContent);
    }

    @Test
    void shouldFailFileDownloadDueFileNotFound() throws Exception {
        generateValidToken();

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/files/" + Integer.MAX_VALUE + "/download")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(Matchers.equalTo("File not found")));
    }

    @Test
    void shouldFailFileDownloadDueNonOwnerUserToken() throws Exception {
        generateValidToken();

        FileResponseDTO file = createFile("File 1", "Description 1");

        String anotherUserToken = generateValidToken("anotherUser");

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/files/" + file.getId() + "/download")
                        .header("Authorization", "Bearer " + anotherUserToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(Matchers.equalTo("File not found")));
    }

    private FileResponseDTO createFile(String name, String description) throws Exception {
        MockMultipartFile mockedFile = new MockMultipartFile("file", name, MediaType.APPLICATION_PDF_VALUE, dummyFile.getInputStream());

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders
                        .multipart("/api/files")
                        .file(mockedFile)
                        .param("description", description)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andReturn();

        return OBJECT_MAPPER.readValue(mvcResult.getResponse().getContentAsString(), FileResponseDTO.class);
    }
}
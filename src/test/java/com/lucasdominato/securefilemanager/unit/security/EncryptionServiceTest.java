package com.lucasdominato.securefilemanager.unit.security;

import com.lucasdominato.securefilemanager.security.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EncryptionServiceTest {

    private static final String VALID_AES_KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String VALID_HMAC_KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String INVALID_AES_KEY = Base64.getEncoder().encodeToString(new byte[16]);
    private static final String INVALID_HMAC_KEY = Base64.getEncoder().encodeToString(new byte[16]);
    private static final int IV_SIZE = 16;

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService(VALID_AES_KEY, VALID_HMAC_KEY);
    }

    @Test
    void testEncryptStreamSuccess() throws GeneralSecurityException, IOException {
        String testData = "Testing phrase for encryption";
        InputStream inputStream = new ByteArrayInputStream(testData.getBytes());

        InputStream encryptedStream = encryptionService.encryptStream(inputStream);

        assertNotNull(encryptedStream);
    }

    @Test
    void testDecryptStreamSuccess() throws GeneralSecurityException, IOException {
        String testData = "Testing phrase for decryption";
        ByteArrayOutputStream encryptedOutputStream = new ByteArrayOutputStream();

        try (InputStream inputStream = new ByteArrayInputStream(testData.getBytes());
             InputStream encryptedStream = encryptionService.encryptStream(inputStream)) {
            encryptedStream.transferTo(encryptedOutputStream);
        }

        ByteArrayInputStream encryptedInputStream = new ByteArrayInputStream(encryptedOutputStream.toByteArray());
        ByteArrayOutputStream decryptedOutputStream = new ByteArrayOutputStream();

        encryptionService.decryptStream(encryptedInputStream, decryptedOutputStream);

        assertEquals(testData, decryptedOutputStream.toString());
    }

    @Test
    void testDecryptStreamWithInvalidHmac() throws GeneralSecurityException, IOException {
        String testData = "Testing phrase for Invalid HMAC";
        ByteArrayOutputStream encryptedOutputStream = new ByteArrayOutputStream();

        try (InputStream inputStream = new ByteArrayInputStream(testData.getBytes());
             InputStream encryptedStream = encryptionService.encryptStream(inputStream)) {
            encryptedStream.transferTo(encryptedOutputStream);
        }

        byte[] encryptedData = encryptedOutputStream.toByteArray();
        encryptedData[encryptedData.length - 1] ^= 1;

        ByteArrayInputStream alteredInputStream = new ByteArrayInputStream(encryptedData);
        ByteArrayOutputStream decryptedOutputStream = new ByteArrayOutputStream();

        assertThrows(SecurityException.class, () -> encryptionService.decryptStream(alteredInputStream, decryptedOutputStream));
    }

    @Test
    void testValidateKeySizeSuccess() {
        assertDoesNotThrow(() -> new EncryptionService(VALID_AES_KEY, VALID_HMAC_KEY));
    }

    @Test
    void testInvalidAesKeySize() {
        assertThrows(IllegalStateException.class, () -> new EncryptionService(INVALID_AES_KEY, VALID_HMAC_KEY));
    }

    @Test
    void testInvalidHmacKeySize() {
        assertThrows(IllegalStateException.class, () -> new EncryptionService(VALID_AES_KEY, INVALID_HMAC_KEY));
    }

    @Test
    void testEncryptStreamWithLargeData() throws GeneralSecurityException, IOException {
        byte[] largeData = new byte[1024 * 1024];
        InputStream largeInputStream = new ByteArrayInputStream(largeData);

        InputStream encryptedStream = encryptionService.encryptStream(largeInputStream);

        assertNotNull(encryptedStream);
    }

    @Test
    void testDecryptStreamWithEmptyInput() {
        byte[] emptyData = new byte[0];
        ByteArrayInputStream emptyInputStream = new ByteArrayInputStream(emptyData);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        assertThrows(IOException.class, () -> encryptionService.decryptStream(emptyInputStream, outputStream));
    }

    @Test
    void testDecryptStreamWithInvalidIv() {
        ByteArrayInputStream invalidIvInputStream = new ByteArrayInputStream(new byte[IV_SIZE - 1]);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        assertThrows(IOException.class, () -> encryptionService.decryptStream(invalidIvInputStream, outputStream));
    }
}
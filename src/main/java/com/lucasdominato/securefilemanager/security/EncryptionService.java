package com.lucasdominato.securefilemanager.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
@Slf4j
public class EncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CTR/NoPadding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int IV_SIZE = 16;
    private static final int BUFFER_SIZE = 8192;
    private static final int HMAC_SIZE = 32;

    private final SecretKey aesKey;
    private final SecretKey hmacKey;
    private final SecureRandom secureRandom;

    public EncryptionService(@Value("${encryption.aes-key}") String base64EncodedAesKey,
                             @Value("${encryption.hmac-key}") String base64EncodedHmacKey) {
        try {
            byte[] decodedAesKey = Base64.getDecoder().decode(base64EncodedAesKey);
            validateKeySize(decodedAesKey.length, "AES");
            this.aesKey = new SecretKeySpec(decodedAesKey, ALGORITHM);

            byte[] decodedHmacKey = Base64.getDecoder().decode(base64EncodedHmacKey);
            validateKeySize(decodedHmacKey.length, "HMAC");
            this.hmacKey = new SecretKeySpec(decodedHmacKey, HMAC_ALGORITHM);

            this.secureRandom = SecureRandom.getInstanceStrong();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize encryption service", e);
        }
    }

    private void validateKeySize(int keySize, String keyType) {
        if (keyType.equals("AES") && keySize != 32) {
            throw new IllegalArgumentException("AES key must be 256 bits");
        }
        if (keyType.equals("HMAC") && keySize < 32) {
            throw new IllegalArgumentException("HMAC key must be at least 256 bits");
        }
    }

    public InputStream encryptStream(InputStream inputStream) throws GeneralSecurityException, IOException {
        byte[] iv = new byte[IV_SIZE];
        secureRandom.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, new IvParameterSpec(iv));

        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(hmacKey);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(iv);
        mac.update(iv);

        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            byte[] encryptedChunk = cipher.update(buffer, 0, bytesRead);
            if (encryptedChunk != null) {
                outputStream.write(encryptedChunk);
                mac.update(encryptedChunk);
            }
        }

        byte[] finalBlock = cipher.doFinal();
        if (finalBlock != null && finalBlock.length > 0) {
            outputStream.write(finalBlock);
            mac.update(finalBlock);
        }

        outputStream.write(mac.doFinal());

        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    public void decryptStream(InputStream encryptedInputStream, OutputStream outputStream)
            throws GeneralSecurityException, IOException {
        byte[] iv = new byte[IV_SIZE];
        if (readFully(encryptedInputStream, iv) != IV_SIZE) {
            throw new IOException("Invalid encrypted data: IV missing");
        }

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, new IvParameterSpec(iv));

        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(hmacKey);
        mac.update(iv);

        ByteArrayOutputStream tempStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;

        long totalBytes = 0;
        while ((bytesRead = encryptedInputStream.read(buffer)) > 0) {
            totalBytes += bytesRead;
            if (totalBytes > Integer.MAX_VALUE) {
                throw new IOException("Input too large");
            }
            tempStream.write(buffer, 0, bytesRead);
        }

        byte[] encryptedData = tempStream.toByteArray();
        if (encryptedData.length < HMAC_SIZE) {
            throw new IOException("Invalid encrypted data: too short");
        }

        int encryptedLength = encryptedData.length - HMAC_SIZE;
        byte[] encryptedContent = Arrays.copyOfRange(encryptedData, 0, encryptedLength);
        byte[] receivedHmac = Arrays.copyOfRange(encryptedData, encryptedLength, encryptedData.length);

        mac.update(encryptedContent);
        byte[] calculatedHmac = mac.doFinal();

        if (!MessageDigest.isEqual(calculatedHmac, receivedHmac)) {
            throw new SecurityException("Data integrity check failed");
        }

        byte[] decryptedData = cipher.doFinal(encryptedContent);
        outputStream.write(decryptedData);
        outputStream.flush();
    }

    private int readFully(InputStream input, byte[] buffer) throws IOException {
        int totalBytesRead = 0;
        int bytesRemaining = buffer.length;
        while (totalBytesRead < buffer.length) {
            int bytesRead = input.read(buffer, totalBytesRead, bytesRemaining);
            if (bytesRead < 0) {
                break;
            }
            totalBytesRead += bytesRead;
            bytesRemaining -= bytesRead;
        }
        return totalBytesRead;
    }
}
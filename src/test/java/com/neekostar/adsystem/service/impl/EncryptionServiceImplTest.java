package com.neekostar.adsystem.service.impl;

import com.neekostar.adsystem.service.EncryptionService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceImplTest {

    private static final String VALID_BASE64_KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String INVALID_BASE64_KEY = "not_a_valid_key";

    @Test
    void testConstructor_ValidKey() {
        EncryptionService service = new EncryptionServiceImpl(VALID_BASE64_KEY);
        assertNotNull(service);
    }

    @Test
    void testConstructor_InvalidKey() {
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                new EncryptionServiceImpl(INVALID_BASE64_KEY));
        assertTrue(exception.getMessage().contains("Failed to init EncryptionService"));
    }

    @Test
    void testEncrypt_NullInput() {
        EncryptionService service = new EncryptionServiceImpl(VALID_BASE64_KEY);
        assertNull(service.encrypt(null));
    }

    @Test
    void testDecrypt_NullInput() {
        EncryptionService service = new EncryptionServiceImpl(VALID_BASE64_KEY);
        assertNull(service.decrypt(null));
    }

    @Test
    void testEncryptDecrypt() {
        EncryptionService service = new EncryptionServiceImpl(VALID_BASE64_KEY);
        String plainText = "Test message";
        String encrypted = service.encrypt(plainText);
        assertNotNull(encrypted);
        String decrypted = service.decrypt(encrypted);
        assertEquals(plainText, decrypted);
    }

    @Test
    void testDecrypt_InvalidData() {
        EncryptionService service = new EncryptionServiceImpl(VALID_BASE64_KEY);
        String invalidEncrypted = Base64.getEncoder().encodeToString("short".getBytes());
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                service.decrypt(invalidEncrypted));
        assertTrue(exception.getMessage().contains("Decryption failed"));
    }

    @Test
    void testEncrypt_Failure() throws Exception {
        EncryptionServiceImpl service = new EncryptionServiceImpl(VALID_BASE64_KEY);
        Field secretKeyField = EncryptionServiceImpl.class.getDeclaredField("secretKey");
        secretKeyField.setAccessible(true);
        secretKeyField.set(service, null);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                service.encrypt("Some message"));
        assertTrue(exception.getMessage().contains("Encryption failed"));
    }

    @Test
    void testDecrypt_Failure() {
        EncryptionServiceImpl service = new EncryptionServiceImpl(VALID_BASE64_KEY);
        String invalidData = Base64.getEncoder().encodeToString(new byte[5]);
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                service.decrypt(invalidData));
        assertTrue(exception.getMessage().contains("Decryption failed"));
    }
}

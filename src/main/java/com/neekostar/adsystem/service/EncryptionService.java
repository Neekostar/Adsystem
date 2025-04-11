package com.neekostar.adsystem.service;

public interface EncryptionService {
    String encrypt(String plainText);

    String decrypt(String encryptedText);
}

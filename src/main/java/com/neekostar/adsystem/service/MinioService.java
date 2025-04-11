package com.neekostar.adsystem.service;

import org.springframework.web.multipart.MultipartFile;

public interface MinioService {
    String uploadFile(MultipartFile file, String folderPrefix);

    void removeFile(String objectName);

    String resolveObjectNameFromUrl(String fileUrl);
}

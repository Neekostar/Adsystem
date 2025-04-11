package com.neekostar.adsystem.service.impl;

import java.io.InputStream;
import java.util.UUID;
import com.neekostar.adsystem.exception.FileStorageException;
import com.neekostar.adsystem.service.MinioService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class MinioServiceImpl implements MinioService {

    private final MinioClient minioClient;
    private final String defaultBucketName;

    @Autowired
    public MinioServiceImpl(MinioClient minioClient,
                            @Value("${minio.bucket-name}") String defaultBucketName) {
        this.minioClient = minioClient;
        this.defaultBucketName = defaultBucketName;
    }

    @Override
    @Retryable(
            backoff = @Backoff(delay = 2000)
    )
    public String uploadFile(MultipartFile file, String folderPrefix) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload an empty or null file");
        }
        try {
            String fileName = generateFileName(file.getOriginalFilename());
            String objectName = (folderPrefix != null && !folderPrefix.isEmpty())
                    ? folderPrefix + "/" + fileName
                    : fileName;

            ensureBucketExists(defaultBucketName);

            try (InputStream inputStream = file.getInputStream()) {
                PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                        .bucket(defaultBucketName)
                        .object(objectName)
                        .stream(inputStream, inputStream.available(), -1)
                        .contentType(file.getContentType())
                        .build();
                minioClient.putObject(putObjectArgs);
            }

            log.info("File {} uploaded successfully to MinIO as {}", file.getOriginalFilename(), objectName);
            return String.format("%s/%s/%s", getEndpointUrl(), defaultBucketName, objectName);
        } catch (Exception e) {
            log.error("Error uploading file to MinIO: {}", e.getMessage());
            throw new FileStorageException("Could not store file " + file.getOriginalFilename() + ". Please try again!", e);
        }
    }

    @Override
    public void removeFile(String objectName) {
        try {
            RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder()
                    .bucket(defaultBucketName)
                    .object(objectName)
                    .build();
            minioClient.removeObject(removeObjectArgs);
            log.info("File {} removed successfully from MinIO", objectName);
        } catch (Exception e) {
            log.error("Common error removing file from MinIO: {}", e.getMessage());
            throw new FileStorageException("Could not delete file " + objectName + ". Please try again!", e);
        }
    }

    @Override
    public String resolveObjectNameFromUrl(@NotNull String fileUrl) {
        String bucketNamePart = "/" + defaultBucketName + "/";
        int bucketNameIndex = fileUrl.indexOf(bucketNamePart);
        if (bucketNameIndex != -1) {
            return fileUrl.substring(bucketNameIndex + bucketNamePart.length());
        }
        throw new FileStorageException("Can`t resolve object name from URL: " + fileUrl);
    }

    private @NotNull String generateFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID() + extension;
    }

    private void ensureBucketExists(String bucketName) throws Exception {
        boolean found =
                minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    @Contract(pure = true)
    private @NotNull String getEndpointUrl() {
        return "http://localhost:9000";
    }
}

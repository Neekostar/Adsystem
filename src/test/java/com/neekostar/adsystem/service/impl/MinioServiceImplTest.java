package com.neekostar.adsystem.service.impl;

import com.neekostar.adsystem.exception.FileStorageException;
import io.minio.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MinioServiceImplTest {

    @Mock
    private MinioClient minioClient;

    private final String defaultBucketName = "test-bucket";

    @InjectMocks
    private MinioServiceImpl minioService;

    @BeforeEach
    void setUp() {
        minioService = new MinioServiceImpl(minioClient, defaultBucketName);
    }

    @Test
    void uploadFile_NullFile_ShouldThrowIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                minioService.uploadFile(null, "folder"));
        assertEquals("Cannot upload an empty or null file", exception.getMessage());
    }

    @Test
    void uploadFile_EmptyFile_ShouldThrowIllegalArgumentException() {
        MultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                minioService.uploadFile(emptyFile, "folder"));
        assertEquals("Cannot upload an empty or null file", exception.getMessage());
    }

    @Test
    void uploadFile_BucketExists_Success() throws Exception {
        String originalFilename = "file.txt";
        byte[] content = "Hello, world!".getBytes();
        MultipartFile file = new MockMultipartFile("file", originalFilename, "text/plain", content);

        BucketExistsArgs existsArgs = BucketExistsArgs.builder().bucket(defaultBucketName).build();
        when(minioClient.bucketExists(eq(existsArgs))).thenReturn(true);

        doAnswer(invocation -> null).when(minioClient).putObject(any(PutObjectArgs.class));

        String url = minioService.uploadFile(file, "folder");
        assertTrue(url.startsWith("http://localhost:9000/" + defaultBucketName + "/folder/"));
    }

    @Test
    void uploadFile_BucketNotExists_Success() throws Exception {
        String originalFilename = "image.png";
        byte[] content = "Image content".getBytes();
        MultipartFile file = new MockMultipartFile("file", originalFilename, "image/png", content);

        BucketExistsArgs existsArgs = BucketExistsArgs.builder().bucket(defaultBucketName).build();
        when(minioClient.bucketExists(eq(existsArgs))).thenReturn(false);
        doAnswer(invocation -> null).when(minioClient).makeBucket(any(MakeBucketArgs.class));
        doAnswer(invocation -> null).when(minioClient).putObject(any(PutObjectArgs.class));

        String url = minioService.uploadFile(file, null);
        assertTrue(url.startsWith("http://localhost:9000/" + defaultBucketName + "/"));
        assertTrue(url.endsWith(".png"));
    }

    @Test
    void uploadFile_PutObjectThrowsException_ShouldThrowFileStorageException() throws Exception {
        String originalFilename = "doc.pdf";
        byte[] content = "PDF content".getBytes();
        MultipartFile file = new MockMultipartFile("file", originalFilename, "application/pdf", content);

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        doThrow(new RuntimeException("Put error")).when(minioClient).putObject(any(PutObjectArgs.class));

        FileStorageException exception = assertThrows(FileStorageException.class, () ->
                minioService.uploadFile(file, "docs"));
        assertTrue(exception.getMessage().contains("Could not store file"));
    }

    @Test
    void removeFile_Success() throws Exception {
        String objectName = "folder/file.txt";
        RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder()
                .bucket(defaultBucketName)
                .object(objectName)
                .build();
        doNothing().when(minioClient).removeObject(eq(removeObjectArgs));

        assertDoesNotThrow(() -> minioService.removeFile(objectName));
        verify(minioClient).removeObject(eq(removeObjectArgs));
    }

    @Test
    void removeFile_MinioException_ShouldThrowFileStorageException() throws Exception {
        String objectName = "file.txt";
        RemoveObjectArgs removeArgs = RemoveObjectArgs.builder()
                .bucket(defaultBucketName)
                .object(objectName)
                .build();
        doThrow(new RuntimeException("Minio error")).when(minioClient).removeObject(eq(removeArgs));

        FileStorageException exception = assertThrows(FileStorageException.class, () ->
                minioService.removeFile(objectName));
        assertTrue(exception.getMessage().contains("Could not delete file"));
    }


    @Test
    void removeFile_GenericException_ShouldThrowFileStorageException() throws Exception {
        String objectName = "file.txt";
        RemoveObjectArgs removeArgs = RemoveObjectArgs.builder()
                .bucket(defaultBucketName)
                .object(objectName)
                .build();
        doThrow(new RuntimeException("Generic error")).when(minioClient).removeObject(eq(removeArgs));

        FileStorageException exception = assertThrows(FileStorageException.class, () ->
                minioService.removeFile(objectName));
        assertTrue(exception.getMessage().contains("Could not delete file"));
    }

    @Test
    void resolveObjectNameFromUrl_Success() {
        String fileUrl = "http://localhost:9000/" + defaultBucketName + "/folder/file.txt";
        String objectName = minioService.resolveObjectNameFromUrl(fileUrl);
        assertEquals("folder/file.txt", objectName);
    }

    @Test
    void resolveObjectNameFromUrl_InvalidUrl_ShouldThrowFileStorageException() {
        String fileUrl = "http://localhost:9000/anotherBucket/folder/file.txt";
        FileStorageException exception = assertThrows(FileStorageException.class, () ->
                minioService.resolveObjectNameFromUrl(fileUrl));
        assertTrue(exception.getMessage().contains("Can`t resolve object name from URL"));
    }
}

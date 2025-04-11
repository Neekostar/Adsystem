package com.neekostar.adsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@Schema(name = "ImageUploadDto", description = "DTO for uploading an image")
public class ImageUploadDto {
    @Schema(description = "Image file to upload")
    private MultipartFile file;
}

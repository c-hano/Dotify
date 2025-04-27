package com.example.dotify.controller;

import com.example.dotify.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    // 📌 1. 도트 PNG 변환
    @PostMapping(value = "/convert", consumes = "multipart/form-data")
    public ResponseEntity<byte[]> convertToPixelArt(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "pixelSize", required = false) Integer pixelSize,
            @RequestParam(value = "colorLevels", required = false) Integer colorLevels
    ) {
        int finalPixelSize = (pixelSize != null) ? pixelSize : 32;       // 기본 32px
        int finalColorLevels = (colorLevels != null) ? colorLevels : 32; // 기본 32색

        byte[] convertedImage = imageService.convertToPixelArt(file, finalPixelSize, finalColorLevels);

        return ResponseEntity
                .ok()
                .header("Content-Type", "image/png")
                .body(convertedImage);
    }

    // 📌 2. 도트 GIF 변환
    @PostMapping(value = "/convert-gif", consumes = "multipart/form-data")
    public ResponseEntity<byte[]> convertToGif(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "pixelSize", required = false) Integer pixelSize,
            @RequestParam(value = "colorLevels", required = false) Integer colorLevels
    ) {
        int finalPixelSize = (pixelSize != null) ? pixelSize : 32;       // 기본 32px
        int finalColorLevels = (colorLevels != null) ? colorLevels : 32; // 기본 32색

        byte[] convertedGif = imageService.convertToGif(file, finalPixelSize, finalColorLevels);

        return ResponseEntity
                .ok()
                .header("Content-Type", "image/gif")
                .body(convertedGif);
    }
}

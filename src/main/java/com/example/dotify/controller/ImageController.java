package com.example.dotify.controller;

import com.example.dotify.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/convert")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<byte[]> convertToPixelArt(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "pixelSize", defaultValue = "32") int pixelSize,
            @RequestParam(value = "colorLevels", defaultValue = "4") int colorLevels
    ) {
        byte[] convertedImage = imageService.convertToPixelArt(file, pixelSize, colorLevels);

        return ResponseEntity
                .ok()
                .header("Content-Type", "image/png")
                .body(convertedImage);
    }
}

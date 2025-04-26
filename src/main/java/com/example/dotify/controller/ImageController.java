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

    @PostMapping
    public ResponseEntity<byte[]> convertToPixelArt(@RequestParam("file") MultipartFile file) {
        byte[] convertedImage = imageService.convertToPixelArt(file);
        return ResponseEntity
                .ok()
                .header("Content-Type", "image/png")
                .body(convertedImage);
    }
}

package com.example.dotify.service;

import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

@Service
@RequiredArgsConstructor
public class ImageService {

    /**
     * 업로드된 이미지를 도트 스타일로 변환하는 메서드
     */
    public byte[] convertToPixelArt(MultipartFile file) {
        try {
            // 1. 업로드된 파일을 BufferedImage로 변환
            BufferedImage originalImage = ImageIO.read(file.getInputStream());

            // 2. 도트화: 이미지를 강제로 작게 줄였다가 다시 키운다
            int pixelSize = 16; // 줄일 크기 (16x16로 줄였다가 다시 확대)

            BufferedImage smallImage = Thumbnails.of(originalImage)
                    .size(pixelSize, pixelSize)
                    .asBufferedImage();

            BufferedImage pixelArtImage = Thumbnails.of(smallImage)
                    .size(originalImage.getWidth(), originalImage.getHeight())
                    .keepAspectRatio(false) // 강제로 원본 크기로 맞추기
                    .asBufferedImage();

            // 3. 결과를 byte[]로 변환해서 반환
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(pixelArtImage, "png", baos);

            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("이미지 변환 실패", e);
        }
    }
}


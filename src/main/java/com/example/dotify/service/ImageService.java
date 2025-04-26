package com.example.dotify.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class ImageService {

    public byte[] convertToPixelArt(MultipartFile file, int pixelSize) {
        try {
            BufferedImage originalImage = ImageIO.read(file.getInputStream());

            // 1. 이미지 줄이기
            Image scaledDown = originalImage.getScaledInstance(
                    pixelSize,
                    pixelSize,
                    Image.SCALE_REPLICATE
            );

            BufferedImage smallImage = new BufferedImage(
                    pixelSize,
                    pixelSize,
                    BufferedImage.TYPE_INT_RGB
            );
            Graphics2D gSmall = smallImage.createGraphics();
            gSmall.drawImage(scaledDown, 0, 0, null);
            gSmall.dispose();

            // 2. 다시 확대
            BufferedImage pixelArtImage = new BufferedImage(
                    originalImage.getWidth(),
                    originalImage.getHeight(),
                    BufferedImage.TYPE_INT_RGB
            );
            Graphics2D g = pixelArtImage.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(smallImage, 0, 0, originalImage.getWidth(), originalImage.getHeight(), null);
            g.dispose();

            // 3. 색상 수 제한 (팔레트화) 추가
            BufferedImage paletteImage = applyColorQuantization(pixelArtImage, 4); // 🔥 4단계로 줄이기 (16색)

            // 4. 이미지 byte[]로 변환
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(paletteImage, "png", baos);
            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("도트 이미지 변환 실패", e);
        }
    }

    // 🎨 색상 수 줄이는 함수
    private BufferedImage applyColorQuantization(BufferedImage image, int levelsPerChannel) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage quantizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        int step = 256 / levelsPerChannel;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(image.getRGB(x, y));

                int r = (color.getRed() / step) * step;
                int g = (color.getGreen() / step) * step;
                int b = (color.getBlue() / step) * step;

                Color newColor = new Color(
                        Math.min(r, 255),
                        Math.min(g, 255),
                        Math.min(b, 255)
                );

                quantizedImage.setRGB(x, y, newColor.getRGB());
            }
        }
        return quantizedImage;
    }
}

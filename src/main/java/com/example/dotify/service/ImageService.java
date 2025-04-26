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

            BufferedImage pixelArtImage = new BufferedImage(
                    originalImage.getWidth(),
                    originalImage.getHeight(),
                    BufferedImage.TYPE_INT_RGB
            );
            Graphics2D g = pixelArtImage.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(smallImage, 0, 0, originalImage.getWidth(), originalImage.getHeight(), null);
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(pixelArtImage, "png", baos);
            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("도트 이미지 변환 실패", e);
        }
    }
}

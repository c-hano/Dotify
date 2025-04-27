package com.example.dotify.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

@Service
@RequiredArgsConstructor
public class GifService {

    public byte[] generateAnimatedGif(BufferedImage baseImage) {
        try {
            int frameCount = 20; // 총 프레임 수
            int moveAmount = 2;  // 이동량 (px)

            int width = baseImage.getWidth();
            int height = baseImage.getHeight();

            // 피사체/배경 마스크 생성
            BufferedImage objectMask = createObjectMask(baseImage);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream);

            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("gif");
            if (!writers.hasNext()) {
                throw new IllegalStateException("GIF writer not found");
            }
            ImageWriter gifWriter = writers.next();
            gifWriter.setOutput(imageOutputStream);
            gifWriter.prepareWriteSequence(null);

            for (int i = 0; i < frameCount; i++) {
                double progress = (2 * Math.PI * i) / frameCount;

                int bgOffsetX = (int) (moveAmount * Math.sin(progress));   // 배경 좌우 흔들림
                int objOffsetY = (int) (moveAmount * Math.cos(progress));  // 피사체 상하 흔들림

                BufferedImage frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = frame.createGraphics();
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, width, height);

                // 1. 배경 먼저 그리기
                g2d.drawImage(shiftImage(baseImage, bgOffsetX, 0, objectMask, false), 0, 0, null);

                // 2. 피사체 그리기 (objectMask가 있는 부분만)
                g2d.drawImage(shiftImage(baseImage, 0, objOffsetY, objectMask, true), 0, 0, null);

                g2d.dispose();

                IIOImage iioImage = new IIOImage(frame, null, getMetadata(gifWriter, frame));
                gifWriter.writeToSequence(iioImage, getWriteParam());
            }

            gifWriter.endWriteSequence();
            imageOutputStream.close();
            return outputStream.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("GIF 생성 실패", e);
        }
    }

    // ✨ 피사체/배경 마스크 만들기 (간단한 밝기 기준)
    private BufferedImage createObjectMask(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage mask = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(image.getRGB(x, y));
                int brightness = (color.getRed() + color.getGreen() + color.getBlue()) / 3;

                if (brightness < 140) { // 밝기 기준 (어두우면 피사체로 간주)
                    mask.setRGB(x, y, Color.WHITE.getRGB());
                } else {
                    mask.setRGB(x, y, Color.BLACK.getRGB());
                }
            }
        }
        return mask;
    }

    // ✨ 이미지를 이동시키는 메서드 (마스크 기반)
    private BufferedImage shiftImage(BufferedImage image, int offsetX, int offsetY, BufferedImage mask, boolean isObject) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage shifted = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean isObjectPixel = new Color(mask.getRGB(x, y)).equals(Color.WHITE);
                if (isObjectPixel == isObject) {
                    int srcX = x - offsetX;
                    int srcY = y - offsetY;
                    if (srcX >= 0 && srcX < width && srcY >= 0 && srcY < height) {
                        shifted.setRGB(x, y, image.getRGB(srcX, srcY));
                    }
                }
            }
        }
        return shifted;
    }

    private IIOMetadata getMetadata(ImageWriter writer, BufferedImage image) throws IOException {
        ImageWriteParam params = writer.getDefaultWriteParam();
        IIOMetadata metadata = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(image), params);
        return metadata;
    }

    private ImageWriteParam getWriteParam() {
        return ImageIO.getImageWritersByFormatName("gif").next().getDefaultWriteParam();
    }
}

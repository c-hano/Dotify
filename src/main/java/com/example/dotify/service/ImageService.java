package com.example.dotify.service;

import java.io.ByteArrayInputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImageService {
    private final GifService gifService; // GIF 생성용 서비스 추가

    public byte[] convertToPixelArt(MultipartFile file, int pixelSize, int colorLevels) {
        try {
            BufferedImage originalImage = ImageIO.read(file.getInputStream());

            // 1. 도트화 (축소)
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

            // 2. 확대 (Nearest Neighbor)
            BufferedImage pixelArtImage = new BufferedImage(
                    originalImage.getWidth(),
                    originalImage.getHeight(),
                    BufferedImage.TYPE_INT_RGB
            );
            Graphics2D g = pixelArtImage.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(smallImage, 0, 0, originalImage.getWidth(), originalImage.getHeight(), null);
            g.dispose();

            // 3. K-means 색상 압축 (32색)
            BufferedImage clusteredImage = applyKMeansColorQuantization(pixelArtImage, 32, 5);

            // 4. 밝기 정규화
            BufferedImage normalizedImage = applyBrightnessNormalization(clusteredImage);

            // 5. 포스터라이징 (16단계)
            BufferedImage posterizedImage = applyPosterization(normalizedImage, 16);

            // 6. 채도/대비 보정
            BufferedImage boostedImage = applySaturationAndContrast(posterizedImage, 1.2, 1.15);

            // 7. 결과 PNG 반환
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(boostedImage, "png", baos);
            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("도트 이미지 변환 실패", e);
        }
    }

    // 변환된 도트 이미지를 기반으로 GIF 만들기
    public byte[] convertToGif(MultipartFile file, int pixelSize, int colorLevels) {
        try {
            byte[] pixelArtBytes = convertToPixelArt(file, pixelSize, colorLevels);
            BufferedImage pixelArtImage = ImageIO.read(new ByteArrayInputStream(pixelArtBytes));
            return gifService.generateAnimatedGif(pixelArtImage);
        } catch (IOException e) {
            throw new RuntimeException("GIF 변환 실패", e);
        }
    }

    // K-means 색상 압축
    private BufferedImage applyKMeansColorQuantization(BufferedImage image, int clusterCount, int iteration) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        List<Color> pixels = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels.add(new Color(image.getRGB(x, y)));
            }
        }

        List<Color> centers = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < clusterCount; i++) {
            centers.add(pixels.get(random.nextInt(pixels.size())));
        }

        for (int iter = 0; iter < iteration; iter++) {
            List<List<Color>> clusters = new ArrayList<>();
            for (int i = 0; i < clusterCount; i++) {
                clusters.add(new ArrayList<>());
            }

            for (Color pixel : pixels) {
                int nearestIndex = 0;
                double nearestDistance = Double.MAX_VALUE;

                for (int i = 0; i < clusterCount; i++) {
                    double distance = colorDistance(pixel, centers.get(i));
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearestIndex = i;
                    }
                }
                clusters.get(nearestIndex).add(pixel);
            }

            for (int i = 0; i < clusterCount; i++) {
                List<Color> cluster = clusters.get(i);
                if (!cluster.isEmpty()) {
                    int rSum = 0, gSum = 0, bSum = 0;
                    for (Color color : cluster) {
                        rSum += color.getRed();
                        gSum += color.getGreen();
                        bSum += color.getBlue();
                    }
                    int rAvg = rSum / cluster.size();
                    int gAvg = gSum / cluster.size();
                    int bAvg = bSum / cluster.size();
                    centers.set(i, new Color(rAvg, gAvg, bAvg));
                }
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color originalColor = new Color(image.getRGB(x, y));
                Color nearestCenter = findNearestCenter(originalColor, centers);
                outputImage.setRGB(x, y, nearestCenter.getRGB());
            }
        }

        return outputImage;
    }

    private Color findNearestCenter(Color color, List<Color> centers) {
        Color nearestColor = centers.get(0);
        double nearestDistance = Double.MAX_VALUE;

        for (Color center : centers) {
            double distance = colorDistance(color, center);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestColor = center;
            }
        }
        return nearestColor;
    }

    private double colorDistance(Color c1, Color c2) {
        int rDiff = c1.getRed() - c2.getRed();
        int gDiff = c1.getGreen() - c2.getGreen();
        int bDiff = c1.getBlue() - c2.getBlue();
        return Math.sqrt(rDiff * rDiff + gDiff * gDiff + bDiff * bDiff);
    }

    // 밝기 정규화
    private BufferedImage applyBrightnessNormalization(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage normalized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        int minBrightness = 255;
        int maxBrightness = 0;

        int[][] brightness = new int[width][height];

        // 1. 밝기 스캔
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(image.getRGB(x, y));
                int value = (color.getRed() + color.getGreen() + color.getBlue()) / 3;
                brightness[x][y] = value;
                minBrightness = Math.min(minBrightness, value);
                maxBrightness = Math.max(maxBrightness, value);
            }
        }

        // 2. 정규화
        double scale = 255.0 / Math.max(1, (maxBrightness - minBrightness));
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(image.getRGB(x, y));

                int r = (int) ((color.getRed() - minBrightness) * scale);
                int g = (int) ((color.getGreen() - minBrightness) * scale);
                int b = (int) ((color.getBlue() - minBrightness) * scale);

                r = Math.min(255, Math.max(0, r));
                g = Math.min(255, Math.max(0, g));
                b = Math.min(255, Math.max(0, b));

                normalized.setRGB(x, y, new Color(r, g, b).getRGB());
            }
        }

        return normalized;
    }

    // 포스터라이징 적용
    private BufferedImage applyPosterization(BufferedImage image, int levelsPerChannel) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

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
                result.setRGB(x, y, newColor.getRGB());
            }
        }
        return result;
    }

    // 채도 + 대비 부드럽게 조정
    private BufferedImage applySaturationAndContrast(BufferedImage image, double saturationFactor, double contrastFactor) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage boostedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color original = new Color(image.getRGB(x, y));
                Color saturated = increaseSaturation(original, saturationFactor);
                Color contrasted = increaseContrast(saturated, contrastFactor);
                boostedImage.setRGB(x, y, contrasted.getRGB());
            }
        }
        return boostedImage;
    }

    private Color increaseSaturation(Color color, double factor) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        hsb[1] = Math.min(1f, hsb[1] * (float) factor); // 채도(Saturation) 증가
        return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
    }

    private Color increaseContrast(Color color, double factor) {
        int r = (int) (((color.getRed() / 255.0 - 0.5) * factor + 0.5) * 255.0);
        int g = (int) (((color.getGreen() / 255.0 - 0.5) * factor + 0.5) * 255.0);
        int b = (int) (((color.getBlue() / 255.0 - 0.5) * factor + 0.5) * 255.0);

        r = Math.min(255, Math.max(0, r));
        g = Math.min(255, Math.max(0, g));
        b = Math.min(255, Math.max(0, b));

        return new Color(r, g, b);
    }
}

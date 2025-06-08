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

            // 3. K-means 색상 압축
            BufferedImage clusteredImage = applyKMeansColorQuantization(pixelArtImage, 32, 5);

            // 4. 밝기 정규화
            BufferedImage normalizedImage = applyBrightnessNormalization(clusteredImage);

            // 5. 디더링 적용
            BufferedImage ditheredImage = applyFloydSteinbergDithering(normalizedImage, colorLevels);

            // 6. 채도/대비 보정
            BufferedImage boostedImage = applySaturationAndContrast(ditheredImage, 1.2, 1.15);

            // 7. 결과 PNG 반환
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(boostedImage, "png", baos);
            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("도트 이미지 변환 실패", e);
        }
    }

    public byte[] convertToGif(MultipartFile file, int pixelSize, int colorLevels) {
        try {
            byte[] pixelArtBytes = convertToPixelArt(file, pixelSize, colorLevels);
            BufferedImage pixelArtImage = ImageIO.read(new ByteArrayInputStream(pixelArtBytes));
            return gifService.generateAnimatedGif(pixelArtImage);
        } catch (IOException e) {
            throw new RuntimeException("GIF 변환 실패", e);
        }
    }

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

    private BufferedImage applyBrightnessNormalization(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage normalized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        int minBrightness = 255;
        int maxBrightness = 0;

        int[][] brightness = new int[width][height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(image.getRGB(x, y));
                int value = (color.getRed() + color.getGreen() + color.getBlue()) / 3;
                brightness[x][y] = value;
                minBrightness = Math.min(minBrightness, value);
                maxBrightness = Math.max(maxBrightness, value);
            }
        }

        double scale = 255.0 / Math.max(1, (maxBrightness - minBrightness));
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(image.getRGB(x, y));

                int r = clamp((int) ((color.getRed() - minBrightness) * scale));
                int g = clamp((int) ((color.getGreen() - minBrightness) * scale));
                int b = clamp((int) ((color.getBlue() - minBrightness) * scale));

                normalized.setRGB(x, y, new Color(r, g, b).getRGB());
            }
        }

        return normalized;
    }

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
        hsb[1] = Math.min(1f, hsb[1] * (float) factor);
        return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
    }

    private Color increaseContrast(Color color, double factor) {
        int r = clamp((int) (((color.getRed() / 255.0 - 0.5) * factor + 0.5) * 255.0));
        int g = clamp((int) (((color.getGreen() / 255.0 - 0.5) * factor + 0.5) * 255.0));
        int b = clamp((int) (((color.getBlue() / 255.0 - 0.5) * factor + 0.5) * 255.0));
        return new Color(r, g, b);
    }

    // ✅ Floyd–Steinberg Dithering
    private BufferedImage applyFloydSteinbergDithering(BufferedImage image, int levelsPerChannel) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage dithered = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        int[][][] error = new int[height][width][3];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color oldColor = new Color(image.getRGB(x, y));
                int r = clamp(oldColor.getRed() + error[y][x][0]);
                int g = clamp(oldColor.getGreen() + error[y][x][1]);
                int b = clamp(oldColor.getBlue() + error[y][x][2]);

                int step = 256 / levelsPerChannel;

                int newR = (r / step) * step;
                int newG = (g / step) * step;
                int newB = (b / step) * step;

                Color newColor = new Color(newR, newG, newB);
                dithered.setRGB(x, y, newColor.getRGB());

                int errR = r - newR;
                int errG = g - newG;
                int errB = b - newB;

                distributeError(error, x, y, errR, errG, errB, width, height);
            }
        }

        return dithered;
    }

    private void distributeError(int[][][] error, int x, int y, int errR, int errG, int errB, int width, int height) {
        applyError(error, x + 1, y,     errR, errG, errB, 7.0 / 16);
        applyError(error, x - 1, y + 1, errR, errG, errB, 3.0 / 16);
        applyError(error, x,     y + 1, errR, errG, errB, 5.0 / 16);
        applyError(error, x + 1, y + 1, errR, errG, errB, 1.0 / 16);
    }

    private void applyError(int[][][] error, int x, int y, int errR, int errG, int errB, double factor) {
        if (x >= 0 && y >= 0 && y < error.length && x < error[0].length) {
            error[y][x][0] += (int) (errR * factor);
            error[y][x][1] += (int) (errG * factor);
            error[y][x][2] += (int) (errB * factor);
        }
    }

    private int clamp(int val) {
        return Math.max(0, Math.min(255, val));
    }
}

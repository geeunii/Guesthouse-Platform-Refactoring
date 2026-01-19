package com.ssg9th2team.geharbang.global.image;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.IIOImage;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

@Component
@Slf4j
public class ImageResizeProcessor {

    private static final int DEFAULT_MAX_WIDTH = 1600;
    private static final int DEFAULT_MAX_HEIGHT = 1600;

    public byte[] resizeKeepingFormat(byte[] imageBytes, String format, int maxWidth, int maxHeight) {
        String normalizedFormat = normalizeFormat(format);
        if (normalizedFormat == null) {
            return imageBytes;
        }
        return resize(imageBytes, normalizedFormat, maxWidth, maxHeight, shouldFillWhite(normalizedFormat));
    }

    public byte[] resizeKeepingFormat(byte[] imageBytes, String format) {
        return resizeKeepingFormat(imageBytes, format, DEFAULT_MAX_WIDTH, DEFAULT_MAX_HEIGHT);
    }

    public byte[] resizeToJpeg(byte[] imageBytes, int maxWidth, int maxHeight) {
        return resize(imageBytes, "jpg", maxWidth, maxHeight, true);
    }

    public String normalizeFormat(String format) {
        if (format == null) return "jpg";
        String lower = format.toLowerCase();
        return switch (lower) {
            case "jpg", "jpeg" -> "jpg";
            case "png" -> "png";
            case "gif" -> "gif";
            default -> null;
        };
    }

    private boolean shouldFillWhite(String format) {
        return "jpg".equals(format);
    }

    private byte[] resize(byte[] imageBytes, String format, int maxWidth, int maxHeight, boolean fillWhiteBackground) {
        if (imageBytes == null || imageBytes.length == 0) {
            return imageBytes;
        }
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (original == null) {
                log.warn("Failed to decode image for resizing. Returning original bytes.");
                return imageBytes;
            }

            Dimension newSize = calculateSize(original.getWidth(), original.getHeight(), maxWidth, maxHeight);
            if (newSize == null) {
                return imageBytes;
            }

            int imageType = determineImageType(format, original);
            BufferedImage resizedImage = new BufferedImage(newSize.width, newSize.height, imageType);
            Graphics2D g2d = resizedImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (fillWhiteBackground) {
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, newSize.width, newSize.height);
            } else if (imageType == BufferedImage.TYPE_INT_ARGB) {
                g2d.setComposite(AlphaComposite.Src);
            }

            g2d.drawImage(original, 0, 0, newSize.width, newSize.height, null);
            g2d.dispose();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            // JPEG 품질 설정 (흐릿함 방지)
            if ("jpg".equals(format)) {
                writeJpegWithQuality(resizedImage, outputStream, 0.95f);
            } else {
                ImageIO.write(resizedImage, format, outputStream);
            }
            
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.warn("Image resizing failed: {}", e.getMessage());
            return imageBytes;
        }
    }
    
    private void writeJpegWithQuality(BufferedImage image, ByteArrayOutputStream outputStream, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            ImageIO.write(image, "jpg", outputStream);
            return;
        }
        
        ImageWriter writer = writers.next();
        ImageWriteParam writeParam = writer.getDefaultWriteParam();
        writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        writeParam.setCompressionQuality(quality);
        
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputStream)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), writeParam);
        } finally {
            writer.dispose();
        }
    }

    private Dimension calculateSize(int width, int height, int maxWidth, int maxHeight) {
        if (width <= maxWidth && height <= maxHeight) {
            return null;
        }
        double scaleX = (double) maxWidth / width;
        double scaleY = (double) maxHeight / height;
        double scale = Math.min(scaleX, scaleY);
        int newWidth = Math.max(1, (int) Math.round(width * scale));
        int newHeight = Math.max(1, (int) Math.round(height * scale));
        return new Dimension(newWidth, newHeight);
    }

    private int determineImageType(String format, BufferedImage original) {
        if ("png".equals(format) && original.getColorModel().hasAlpha()) {
            return BufferedImage.TYPE_INT_ARGB;
        }
        return BufferedImage.TYPE_INT_RGB;
    }
}

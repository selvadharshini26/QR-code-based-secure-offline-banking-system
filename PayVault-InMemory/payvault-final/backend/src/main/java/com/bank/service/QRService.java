package com.bank.service;

import com.google.gson.Gson;
import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class QRService {

    private static final Gson GSON = new Gson();
    private final CryptoService crypto = new CryptoService();

    /**
     * Generate a QR code PNG as Base64 string.
     * Payload is encrypted before encoding.
     */
    public String generateQR(Map<String, Object> payload) throws Exception {
        String json      = GSON.toJson(payload);
        String encrypted = crypto.encrypt(json);

        QRCodeWriter writer = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 2);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        BitMatrix matrix = writer.encode(encrypted, BarcodeFormat.QR_CODE, 300, 300, hints);
        BufferedImage raw = MatrixToImageWriter.toBufferedImage(matrix);

        // Style: dark teal on dark background
        BufferedImage styled = styleQR(raw);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(styled, "PNG", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    /** Generate QR with plain JSON (no encryption) - for frontend display */
    public String generateQRPlain(Map<String, Object> payload) throws Exception {
        String json = GSON.toJson(payload);

        QRCodeWriter writer = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 2);

        BitMatrix matrix = writer.encode(json, BarcodeFormat.QR_CODE, 300, 300, hints);
        BufferedImage raw = MatrixToImageWriter.toBufferedImage(matrix);
        BufferedImage styled = styleQR(raw);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(styled, "PNG", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    /** Parse a QR payload - tries JSON first, then decryption */
    public Map<String, Object> parseQR(String qrText) throws Exception {
        // Try JSON first
        try {
            Map<?, ?> raw = GSON.fromJson(qrText, Map.class);
            Map<String, Object> result = new HashMap<>();
            raw.forEach((k, v) -> result.put(k.toString(), v));
            return result;
        } catch (Exception ignored) {}

        // Try decryption
        try {
            String decrypted = crypto.decrypt(qrText);
            Map<?, ?> raw = GSON.fromJson(decrypted, Map.class);
            Map<String, Object> result = new HashMap<>();
            raw.forEach((k, v) -> result.put(k.toString(), v));
            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid QR code data");
        }
    }

    // Teal-on-dark styling
    private BufferedImage styleQR(BufferedImage raw) {
        int w = raw.getWidth(), h = raw.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Color bg   = new Color(13, 17, 23);    // #0d1117
        Color dark = new Color(0, 212, 170);   // #00d4aa teal
        Color light= new Color(13, 17, 23);    // same as bg
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int pixel = raw.getRGB(x, y);
                out.setRGB(x, y, (pixel == 0xFF000000) ? dark.getRGB() : light.getRGB());
            }
        }
        return out;
    }
}

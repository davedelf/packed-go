package com.packed_go.event_service.services.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.packed_go.event_service.services.QRCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Implementation of QRCodeService using ZXing library
 */
@Service
@Slf4j
public class QRCodeServiceImpl implements QRCodeService {

    private static final int DEFAULT_WIDTH = 300;
    private static final int DEFAULT_HEIGHT = 300;
    private static final String IMAGE_FORMAT = "PNG";

    @Override
    public String generateQRCodeBase64(String data, int width, int height) {
        try {
            log.debug("Generating QR code for data: {}", data);
            
            // Create QR Code writer
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            
            // Generate QR code bit matrix
            BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height);
            
            // Convert bit matrix to buffered image
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            
            // Convert image to Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(qrImage, IMAGE_FORMAT, baos);
            byte[] imageBytes = baos.toByteArray();
            
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            
            log.info("✅ QR code generated successfully (size: {}x{})", width, height);
            return "data:image/png;base64," + base64Image;
            
        } catch (WriterException e) {
            log.error("❌ Failed to generate QR code: WriterException - {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate QR code", e);
        } catch (IOException e) {
            log.error("❌ Failed to convert QR code to image: IOException - {}", e.getMessage(), e);
            throw new RuntimeException("Failed to convert QR code to image", e);
        }
    }

    @Override
    public String generateQRCodeBase64(String data) {
        return generateQRCodeBase64(data, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }
}

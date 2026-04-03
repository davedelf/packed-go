package com.packed_go.event_service.services;

/**
 * Service for generating QR codes for tickets
 */
public interface QRCodeService {
    
    /**
     * Generates a QR code as a Base64-encoded PNG image
     * @param data The data to encode in the QR code
     * @param width The width of the QR code image in pixels
     * @param height The height of the QR code image in pixels
     * @return Base64-encoded PNG image string
     */
    String generateQRCodeBase64(String data, int width, int height);
    
    /**
     * Generates a QR code with default dimensions (300x300)
     * @param data The data to encode in the QR code
     * @return Base64-encoded PNG image string
     */
    String generateQRCodeBase64(String data);
}

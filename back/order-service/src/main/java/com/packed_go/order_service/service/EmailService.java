package com.packed_go.order_service.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.packed_go.order_service.dto.external.EventDTO;
import com.packed_go.order_service.dto.external.TicketWithConsumptionsResponse;
import com.packed_go.order_service.entity.Order;
import com.packed_go.order_service.entity.OrderItem;
import com.packed_go.order_service.entity.OrderItemConsumption;
import com.packed_go.order_service.external.EventServiceClient;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final EventServiceClient eventServiceClient;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${event-service.url:http://event-service:8086}")
    private String eventServiceUrl;

    @Async
    public void sendOrderConfirmation(Order order, String toEmail, List<TicketWithConsumptionsResponse> generatedTickets) {
        if (toEmail == null || toEmail.isEmpty()) {
            log.warn("⚠️ Cannot send order confirmation: No email provided for order {}", order.getOrderNumber());
            return;
        }

        log.info("📧 Sending order confirmation email to {} for order {}", toEmail, order.getOrderNumber());

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("✅ Confirmación de Compra - PackedGo - " + order.getOrderNumber());

            // Convertir tickets generados a TicketInfo
            List<TicketInfo> ticketsInfo = convertToTicketInfo(generatedTickets, order);

            String htmlContent = buildOrderConfirmationHtml(order, ticketsInfo);
            helper.setText(htmlContent, true);

            // Adjuntar QR codes como imágenes inline
            attachQRCodes(helper, ticketsInfo);

            mailSender.send(message);
            log.info("✅ Order confirmation email sent successfully to {} with {} QR codes", toEmail, ticketsInfo.size());

        } catch (Exception e) {
            log.error("❌ Failed to send order confirmation email: {}", e.getMessage(), e);
        }
    }

    /**
     * Convierte la lista de tickets generados a TicketInfo para el email
     * Obtiene los nombres de eventos desde el EventServiceClient
     */
    private List<TicketInfo> convertToTicketInfo(List<TicketWithConsumptionsResponse> tickets, Order order) {
        log.info("🔄 Converting {} tickets to TicketInfo for order {}", tickets.size(), order.getOrderNumber());

        List<TicketInfo> ticketsInfo = new ArrayList<>();
        int ticketCounter = 1;
        
        // Crear un mapa de eventId -> eventName para evitar múltiples llamadas
        Map<Long, String> eventNamesCache = new HashMap<>();
        
        for (TicketWithConsumptionsResponse ticket : tickets) {
            try {
                String qrCode = ticket.getQrCode();
                
                if (qrCode != null && !qrCode.isEmpty()) {
                    // Obtener nombre del evento desde los OrderItems
                    String eventName = "Evento";
                    for (OrderItem item : order.getItems()) {
                        // Intentar obtener el evento desde el EventServiceClient
                        if (!eventNamesCache.containsKey(item.getEventId())) {
                            try {
                                EventDTO eventDTO = eventServiceClient.getEventById(item.getEventId());
                                if (eventDTO != null && eventDTO.getName() != null) {
                                    eventNamesCache.put(item.getEventId(), eventDTO.getName());
                                }
                            } catch (Exception e) {
                                log.warn("⚠️ Could not fetch event name for ID {}: {}", item.getEventId(), e.getMessage());
                                eventNamesCache.put(item.getEventId(), "Evento ID: " + item.getEventId());
                            }
                        }
                        eventName = eventNamesCache.getOrDefault(item.getEventId(), "Evento ID: " + item.getEventId());
                        break; // Solo tomar el primer item por ahora
                    }
                    
                    TicketInfo info = new TicketInfo();
                    info.setQrCode(qrCode);
                    info.setEventName(eventName);
                    info.setTicketNumber(ticketCounter++);
                    info.setPassCode(ticket.getPassCode());
                    ticketsInfo.add(info);
                    log.debug("✅ Converted ticket #{} - QR: {}... for event: {}", 
                              ticketCounter - 1, 
                              qrCode.substring(0, Math.min(qrCode.length(), 20)) + "...", 
                              eventName);
                }
            } catch (Exception e) {
                log.warn("⚠️ Error converting ticket data: {}", e.getMessage());
            }
        }
        
        log.info("✅ Converted {} tickets successfully", ticketsInfo.size());
        return ticketsInfo;
    }

    private void attachQRCodes(MimeMessageHelper helper, List<TicketInfo> tickets) throws MessagingException {
        for (TicketInfo ticket : tickets) {
            try {
                byte[] qrImage = generateQRCodeImageWithCode(ticket.getQrCode(), ticket.getPassCode());
                String cid = "qr" + ticket.getTicketNumber();
                helper.addInline(cid, new ByteArrayResource(qrImage), "image/png");
            } catch (Exception e) {
                log.error("Error attaching QR code for ticket {}: {}", ticket.getTicketNumber(), e.getMessage());
            }
        }
    }

    private byte[] generateQRCodeImage(String qrText) throws Exception {
        // Si el QR code ya es una imagen base64 (data:image/png;base64,...)
        if (qrText != null && qrText.startsWith("data:image/")) {
            log.debug("🖼️ QR code is already a base64 image, decoding it");
            String base64Data = qrText.substring(qrText.indexOf(",") + 1);
            return java.util.Base64.getDecoder().decode(base64Data);
        }
        
        // Si es texto plano, generar el QR code
        if (qrText != null && !qrText.isEmpty()) {
            log.debug("📝 Generating QR code from text: {}", qrText.substring(0, Math.min(qrText.length(), 30)) + "...");
        }
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix bitMatrix = qrCodeWriter.encode(qrText, BarcodeFormat.QR_CODE, 300, 300, hints);
        BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(qrImage, "PNG", baos);
        return baos.toByteArray();
    }

    /**
     * Genera una imagen QR con el código de pase (últimos 8 dígitos) debajo
     */
    private byte[] generateQRCodeImageWithCode(String qrText, String passCode) throws Exception {
        // Primero generar el QR base
        byte[] baseQRImage = generateQRCodeImage(qrText);
        
        // Cargar la imagen QR base
        BufferedImage qrImage = ImageIO.read(new java.io.ByteArrayInputStream(baseQRImage));
        
        // Obtener los últimos 8 caracteres del código
        String last8Digits = passCode != null && passCode.length() >= 8 
            ? passCode.substring(passCode.length() - 8) 
            : passCode;
        
        // Crear una nueva imagen más grande para incluir el texto
        int margin = 40;
        int textHeight = 50;
        int newWidth = qrImage.getWidth() + (margin * 2);
        int newHeight = qrImage.getHeight() + (margin * 2) + textHeight;
        
        BufferedImage finalImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2d = finalImage.createGraphics();
        
        // Configurar renderizado de alta calidad
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Fondo blanco
        g2d.setColor(java.awt.Color.WHITE);
        g2d.fillRect(0, 0, newWidth, newHeight);
        
        // Dibujar el QR centrado
        int qrX = margin;
        int qrY = margin;
        g2d.drawImage(qrImage, qrX, qrY, null);
        
        // Dibujar el texto del código
        g2d.setColor(java.awt.Color.BLACK);
        g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 20));
        
        String codeText = "Código: " + last8Digits;
        java.awt.FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(codeText);
        int textX = (newWidth - textWidth) / 2;
        int textY = qrImage.getHeight() + margin + 35;
        
        g2d.drawString(codeText, textX, textY);
        
        g2d.dispose();
        
        // Convertir a bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(finalImage, "PNG", baos);
        
        log.debug("✅ Generated QR image with code: {}", last8Digits);
        return baos.toByteArray();
    }

    private String buildOrderConfirmationHtml(Order order, List<TicketInfo> tickets) {
        StringBuilder sb = new StringBuilder();
        
        // Colores de la landing page
        String primaryGradient = "linear-gradient(135deg, #667eea 0%, #764ba2 100%)";
        String primaryColor = "#667eea";
        
        sb.append("<!DOCTYPE html>");
        sb.append("<html lang='es'>");
        sb.append("<head>");
        sb.append("<meta charset='UTF-8'>");
        sb.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        sb.append("<title>Confirmación de Compra - PackedGo</title>");
        sb.append("</head>");
        sb.append("<body style='margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Oxygen, Ubuntu, Cantarell, sans-serif; background-color: #f5f7fa;'>");
        
        // Container principal
        sb.append("<div style='max-width: 650px; margin: 40px auto; background: white; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 40px rgba(0,0,0,0.1);'>");
        
        // Header con gradiente
        sb.append("<div style='background: ").append(primaryGradient).append("; padding: 40px 30px; text-align: center;'>");
        sb.append("<h1 style='color: white; font-size: 32px; margin: 0 0 10px 0; font-weight: 900; text-shadow: 2px 2px 4px rgba(0,0,0,0.2);'>¡Gracias por tu compra! 🎉</h1>");
        sb.append("<p style='color: rgba(255,255,255,0.95); font-size: 18px; margin: 0; font-weight: 300;'>Tu orden ha sido confirmada exitosamente</p>");
        sb.append("</div>");
        
        // Contenido principal
        sb.append("<div style='padding: 40px 30px;'>");
        
        // Número de orden destacado
        sb.append("<div style='background: linear-gradient(135deg, #667eea15 0%, #764ba215 100%); border-left: 4px solid ").append(primaryColor).append("; padding: 20px; border-radius: 8px; margin-bottom: 30px;'>");
        sb.append("<p style='margin: 0; color: #555; font-size: 14px;'>Número de Orden</p>");
        sb.append("<p style='margin: 5px 0 0 0; color: ").append(primaryColor).append("; font-size: 24px; font-weight: 700;'>").append(order.getOrderNumber()).append("</p>");
        sb.append("</div>");
        
        // Resumen de la orden
        sb.append("<div style='background: #f9fafb; padding: 25px; border-radius: 12px; margin-bottom: 30px;'>");
        sb.append("<h2 style='color: #2c3e50; font-size: 20px; margin: 0 0 20px 0; font-weight: 700;'>📋 Resumen de tu Compra</h2>");
        
        sb.append("<div style='display: flex; justify-content: space-between; margin-bottom: 12px;'>");
        sb.append("<span style='color: #666; font-size: 15px;'>Fecha de compra:</span>");
        sb.append("<span style='color: #333; font-weight: 600; font-size: 15px;'>").append(order.getPaidAt() != null ? order.getPaidAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A").append("</span>");
        sb.append("</div>");
        
        sb.append("<div style='border-top: 2px solid #e5e7eb; margin: 15px 0; padding-top: 15px;'>");
        sb.append("<div style='display: flex; justify-content: space-between; align-items: center;'>");
        sb.append("<span style='color: #333; font-size: 18px; font-weight: 700;'>Total Pagado:</span>");
        sb.append("<span style='color: ").append(primaryColor).append("; font-size: 28px; font-weight: 900;'>$").append(String.format("%,.2f", order.getTotalAmount())).append("</span>");
        sb.append("</div>");
        sb.append("</div>");
        sb.append("</div>");
        
        // Detalle de entradas
        sb.append("<h2 style='color: #2c3e50; font-size: 20px; margin: 0 0 20px 0; font-weight: 700;'>🎫 Detalle de tus Entradas</h2>");
        
        BigDecimal totalGeneral = BigDecimal.ZERO;
        
        for (OrderItem item : order.getItems()) {
            String eventName = getEventName(item.getEventId());
            
            // Tabla para cada entrada
            sb.append("<table style='width: 100%; border-collapse: collapse; margin-bottom: 25px; border: 1px solid #e5e7eb; border-radius: 8px; overflow: hidden;'>");
            
            // Header de la entrada
            sb.append("<thead>");
            sb.append("<tr style='background: ").append(primaryGradient).append(";'>");
            sb.append("<th style='padding: 15px; text-align: left; color: white; font-weight: 700; font-size: 14px;'>Concepto</th>");
            sb.append("<th style='padding: 15px; text-align: center; color: white; font-weight: 700; font-size: 14px;'>Cant.</th>");
            sb.append("<th style='padding: 15px; text-align: right; color: white; font-weight: 700; font-size: 14px;'>Precio Unit.</th>");
            sb.append("<th style='padding: 15px; text-align: right; color: white; font-weight: 700; font-size: 14px;'>Subtotal</th>");
            sb.append("</tr>");
            sb.append("</thead>");
            sb.append("<tbody>");
            
            // Fila de la entrada principal
            BigDecimal entradaSubtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            sb.append("<tr style='background: #f9fafb;'>");
            sb.append("<td style='padding: 15px; color: #333; font-weight: 700;'>🎟️ ").append(eventName).append("</td>");
            sb.append("<td style='padding: 15px; text-align: center; color: #666; font-weight: 600;'>").append(item.getQuantity()).append("</td>");
            sb.append("<td style='padding: 15px; text-align: right; color: #666; font-weight: 600;'>$").append(String.format("%,.2f", item.getUnitPrice())).append("</td>");
            sb.append("<td style='padding: 15px; text-align: right; color: ").append(primaryColor).append("; font-weight: 700;'>$").append(String.format("%,.2f", entradaSubtotal)).append("</td>");
            sb.append("</tr>");
            
            // Filas de consumiciones asociadas
            if (item.getConsumptions() != null && !item.getConsumptions().isEmpty()) {
                sb.append("<tr style='background: #fff;'>");
                sb.append("<td colspan='4' style='padding: 10px 15px 5px 15px;'>");
                sb.append("<span style='color: #667eea; font-weight: 600; font-size: 13px;'>📦 Consumiciones incluidas:</span>");
                sb.append("</td>");
                sb.append("</tr>");
                
                for (OrderItemConsumption consumption : item.getConsumptions()) {
                    sb.append("<tr style='background: #fff; border-bottom: 1px solid #f3f4f6;'>");
                    sb.append("<td style='padding: 8px 15px 8px 35px; color: #666; font-size: 13px;'>").append(consumption.getConsumptionName()).append("</td>");
                    sb.append("<td style='padding: 8px 15px; text-align: center; color: #888; font-size: 13px;'>").append(consumption.getQuantity()).append("</td>");
                    sb.append("<td style='padding: 8px 15px; text-align: right; color: #888; font-size: 13px;'>$").append(String.format("%,.2f", consumption.getUnitPrice())).append("</td>");
                    sb.append("<td style='padding: 8px 15px; text-align: right; color: #666; font-size: 13px;'>$").append(String.format("%,.2f", consumption.getSubtotal())).append("</td>");
                    sb.append("</tr>");
                }
            }
            
            // Fila de subtotal del item (entrada + consumiciones)
            sb.append("<tr style='background: #f3f4f6;'>");
            sb.append("<td colspan='3' style='padding: 12px 15px; text-align: right; color: #333; font-weight: 700;'>Subtotal:</td>");
            sb.append("<td style='padding: 12px 15px; text-align: right; color: ").append(primaryColor).append("; font-weight: 700; font-size: 16px;'>$").append(String.format("%,.2f", item.getSubtotal())).append("</td>");
            sb.append("</tr>");
            
            sb.append("</tbody>");
            sb.append("</table>");
            
            totalGeneral = totalGeneral.add(item.getSubtotal());
        }
        
        // Total General
        sb.append("<div style='background: ").append(primaryGradient).append("; padding: 20px; border-radius: 8px; margin-bottom: 30px;'>");
        sb.append("<div style='display: flex; justify-content: space-between; align-items: center;'>");
        sb.append("<span style='color: white; font-size: 20px; font-weight: 700;'>💰 TOTAL GENERAL:</span>");
        sb.append("<span style='color: white; font-size: 32px; font-weight: 900;'>$").append(String.format("%,.2f", totalGeneral)).append("</span>");
        sb.append("</div>");
        sb.append("</div>");
        
        // Sección de QR Codes
        if (!tickets.isEmpty()) {
            sb.append("<div style='background: linear-gradient(135deg, #667eea08 0%, #764ba208 100%); padding: 30px; border-radius: 12px; margin-bottom: 30px; border: 2px solid #667eea20;'>");
            sb.append("<h2 style='color: #2c3e50; font-size: 20px; margin: 0 0 15px 0; font-weight: 700; text-align: center;'>📱 Tus Códigos QR</h2>");
            sb.append("<p style='color: #666; text-align: center; margin: 0 0 25px 0; font-size: 14px;'>Presenta estos códigos QR en el evento para acceder</p>");
            
            for (TicketInfo ticket : tickets) {
                sb.append("<div style='background: white; padding: 20px; border-radius: 10px; margin-bottom: 20px; text-align: center; box-shadow: 0 2px 8px rgba(0,0,0,0.05);'>");
                sb.append("<p style='margin: 0 0 15px 0; color: ").append(primaryColor).append("; font-weight: 700; font-size: 16px;'>🎟️ ").append(ticket.getEventName()).append("</p>");
                sb.append("<img src='cid:qr").append(ticket.getTicketNumber()).append("' alt='QR Code' style='max-width: 250px; width: 100%; height: auto; border: 3px solid ").append(primaryColor).append("; border-radius: 8px; padding: 10px; background: white;'/>");
                sb.append("<p style='margin: 15px 0 0 0; color: #999; font-size: 12px;'>Ticket #").append(ticket.getTicketNumber()).append("</p>");
                sb.append("</div>");
            }
            sb.append("</div>");
        }
        
        // Instrucciones
        sb.append("<div style='background: #fef3c7; border-left: 4px solid #f59e0b; padding: 20px; border-radius: 8px; margin-bottom: 30px;'>");
        sb.append("<p style='margin: 0; color: #92400e; font-size: 14px; line-height: 1.6;'>");
        sb.append("💡 <strong>Importante:</strong> También puedes acceder a tus tickets en cualquier momento desde la sección <strong>\"Mis Tickets\"</strong> en la aplicación PackedGo.");
        sb.append("</p>");
        sb.append("</div>");
        
        // Call to action
        sb.append("<div style='text-align: center; margin: 30px 0;'>");
        sb.append("<a href='http://localhost:3000/customer/dashboard?tab=tickets' style='display: inline-block; background: ").append(primaryGradient).append("; color: white; padding: 15px 40px; text-decoration: none; border-radius: 25px; font-weight: 700; font-size: 16px; box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);'>Ver Mis Tickets</a>");
        sb.append("</div>");
        
        sb.append("</div>"); // End padding content
        
        // Footer
        sb.append("<div style='background: #f9fafb; padding: 30px; text-align: center; border-top: 1px solid #e5e7eb;'>");
        sb.append("<p style='margin: 0 0 10px 0; color: #666; font-size: 14px;'>¿Necesitas ayuda? Contáctanos en <a href='mailto:soporte@packedgo.com' style='color: ").append(primaryColor).append("; text-decoration: none;'>soporte@packedgo.com</a></p>");
        sb.append("<p style='margin: 10px 0 0 0; color: #999; font-size: 12px;'>&copy; 2025 PackedGo Events. Todos los derechos reservados.</p>");
        sb.append("</div>");
        
        sb.append("</div>"); // End container
        sb.append("</body>");
        sb.append("</html>");
        
        return sb.toString();
    }

    private String getEventName(Long eventId) {
        try {
            EventDTO event = eventServiceClient.getEventById(eventId);
            return event != null ? event.getName() : "Evento ID: " + eventId;
        } catch (Exception e) {
            log.warn("Could not fetch event name for eventId {}: {}", eventId, e.getMessage());
            return "Evento ID: " + eventId;
        }
    }

    // Inner class para almacenar información de tickets
    private static class TicketInfo {
        private String qrCode;
        private String eventName;
        private int ticketNumber;
        private String passCode;

        public String getQrCode() { return qrCode; }
        public void setQrCode(String qrCode) { this.qrCode = qrCode; }
        public String getEventName() { return eventName; }
        public void setEventName(String eventName) { this.eventName = eventName; }
        public int getTicketNumber() { return ticketNumber; }
        public void setTicketNumber(int ticketNumber) { this.ticketNumber = ticketNumber; }
        public String getPassCode() { return passCode; }
        public void setPassCode(String passCode) { this.passCode = passCode; }
    }
}

package com.packedgo.payment_service.client;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

/**
 * Cliente para comunicarse con order-service
 */
@Component
@Slf4j
public class OrderServiceClient {

    private final RestTemplate restTemplate;
    private final String orderServiceUrl;

    public OrderServiceClient(
            RestTemplate restTemplate,
            @Value("${order.service.url:http://order-service:8084}") String orderServiceUrl) {
        this.restTemplate = restTemplate;
        this.orderServiceUrl = orderServiceUrl;
    }

    /**
     * Notifica a order-service que un pago fue aprobado
     * 
     * @param orderNumber Número de la orden (ej: ORD-202510-123)
     * @param paymentId ID del pago de MercadoPago
     * @param customerEmail Email del cliente (opcional)
     * @return true si la notificación fue exitosa
     */
    public boolean notifyPaymentApproved(String orderNumber, Long paymentId, String customerEmail) {
        String url = orderServiceUrl + "/api/orders/payment-callback";

        try {
            log.info("Notificando a order-service: orderNumber={}, paymentId={}, email={}", orderNumber, paymentId, customerEmail);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = new java.util.HashMap<>(Map.of(
                    "orderNumber", orderNumber,
                    "mpPaymentId", paymentId,
                    "paymentStatus", "APPROVED"));
            
            if (customerEmail != null) {
                request.put("customerEmail", customerEmail);
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    (Class<Map<String, Object>>) (Class<?>) Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Orden {} actualizada exitosamente en order-service", orderNumber);
                return true;
            } else {
                log.error("Error al actualizar orden {}: {}", orderNumber, response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            log.error("Error comunicándose con order-service para orden {}: {}", orderNumber, e.getMessage());
            return false;
        }
    }

    /**
     * Notifica a order-service que un pago fue rechazado
     */
    public boolean notifyPaymentRejected(String orderNumber, Long paymentId, String reason) {
        String url = orderServiceUrl + "/api/orders/payment-callback";

        try {
            log.info("Notificando rechazo a order-service: orderNumber={}, paymentId={}", orderNumber, paymentId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = Map.of(
                    "orderNumber", orderNumber,
                    "mpPaymentId", paymentId,
                    "paymentStatus", "REJECTED");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    (Class<Map<String, Object>>) (Class<?>) Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Orden {} notificada como rechazada en order-service", orderNumber);
                return true;
            } else {
                log.error("Error al notificar rechazo de orden {}: {}", orderNumber, response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            log.error("Error comunicándose con order-service para orden {}: {}", orderNumber, e.getMessage());
            return false;
        }
    }
}

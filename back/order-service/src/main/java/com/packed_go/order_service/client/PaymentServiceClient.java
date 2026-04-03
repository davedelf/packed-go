package com.packed_go.order_service.client;

import com.packed_go.order_service.dto.external.PaymentServiceRequest;
import com.packed_go.order_service.dto.external.PaymentServiceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${app.services.payment-service.base-url}")
    private String paymentServiceBaseUrl;
    
    /**
     * Crea un pago en payment-service (LEGACY - MercadoPago)
     */
    public PaymentServiceResponse createPayment(PaymentServiceRequest request) {
        // Fix: paymentServiceBaseUrl already includes /api from .env
        String url = paymentServiceBaseUrl + "/payments/create";
        
        log.info("Calling payment-service: POST {} with orderId: {}", url, request.getOrderId());
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<PaymentServiceRequest> entity = new HttpEntity<>(request, headers);
        
        try {
            PaymentServiceResponse response = restTemplate.postForObject(
                    url,
                    entity,
                    PaymentServiceResponse.class
            );
            
            log.info("Payment created successfully. PreferenceId: {}", response.getPreferenceId());
            return response;
            
        } catch (Exception e) {
            log.error("Error calling payment-service: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create payment: " + e.getMessage(), e);
        }
    }

    /**
     * Crea un pago con Stripe en payment-service (RECOMENDADO)
     */
    public PaymentServiceResponse createPaymentStripe(PaymentServiceRequest request) {
        // Fix: paymentServiceBaseUrl already includes /api from .env
        String url = paymentServiceBaseUrl + "/payments/create-checkout-stripe";
        
        log.info("🔷 Calling payment-service Stripe: POST {} with orderId: {}", url, request.getOrderId());
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<PaymentServiceRequest> entity = new HttpEntity<>(request, headers);
        
        try {
            PaymentServiceResponse response = restTemplate.postForObject(
                    url,
                    entity,
                    PaymentServiceResponse.class
            );
            
            log.info("✅ Stripe payment created successfully. SessionId: {}, CheckoutUrl: {}", 
                response.getSessionId(), response.getCheckoutUrl());
            return response;
            
        } catch (Exception e) {
            log.error("❌ Error calling payment-service Stripe: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create Stripe payment: " + e.getMessage(), e);
        }
    }
    
    /**
     * Obtiene el estado de un pago por orderId
     */
    public PaymentServiceResponse getPaymentByOrderId(String orderId) {
        // Fix: paymentServiceBaseUrl already includes /api from .env
        String url = paymentServiceBaseUrl + "/payments/order/" + orderId;
        
        log.info("Calling payment-service: GET {}", url);
        
        try {
            return restTemplate.getForObject(url, PaymentServiceResponse.class);
        } catch (Exception e) {
            log.error("Error getting payment status: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get payment status: " + e.getMessage(), e);
        }
    }
}

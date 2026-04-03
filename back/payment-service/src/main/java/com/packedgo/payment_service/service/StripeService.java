package com.packedgo.payment_service.service;

import com.packedgo.payment_service.dto.StripeCheckoutRequest;
import com.packedgo.payment_service.dto.StripeCheckoutResponse;
import com.packedgo.payment_service.exception.PaymentException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class StripeService {
    
    @Value("${stripe.secret.key}")
    private String stripeSecretKey;
    
    @Value("${stripe.webhook.secret:}")
    private String webhookSecret;
    
    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrl;
    
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
        log.info("Stripe API initialized");
    }
    
    public StripeCheckoutResponse createCheckoutSession(StripeCheckoutRequest request) {
        log.info("Creating Stripe checkout session for order: {}", request.getOrderId());
        
        try {
            // Construir line items
            List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();
            
            for (StripeCheckoutRequest.ItemDTO item : request.getItems()) {
                SessionCreateParams.LineItem.PriceData.ProductData productData = 
                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName(item.getName())
                        .setDescription(item.getDescription())
                        .build();
                
                // Convertir ARS a USD (1 USD = 1500 ARS)
                // Cents = (ARS / 1500) * 100 = ARS / 15
                long unitAmountCents = item.getUnitPrice()
                    .divide(java.math.BigDecimal.valueOf(15), 0, java.math.RoundingMode.HALF_UP)
                    .longValue();
                
                // Stripe requiere al menos 50 centavos de USD
                if (unitAmountCents < 50) {
                    unitAmountCents = 50; 
                }

                SessionCreateParams.LineItem.PriceData priceData =
                    SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency("usd")
                        .setUnitAmount(unitAmountCents)
                        .setProductData(productData)
                        .build();
                
                SessionCreateParams.LineItem lineItem = 
                    SessionCreateParams.LineItem.builder()
                        .setPriceData(priceData)
                        .setQuantity(Long.valueOf(item.getQuantity()))
                        .build();
                
                lineItems.add(lineItem);
            }
            
            // Metadata para tracking
            Map<String, String> metadata = new HashMap<>();
            metadata.put("orderId", request.getOrderId());
            metadata.put("customerEmail", request.getCustomerEmail());
            
            // Construir URLs
            String successUrl = request.getSuccessUrl() != null ? 
                request.getSuccessUrl() : 
                frontendUrl + "/customer/orders/success?orderId=" + request.getOrderId() + "&sessionId={CHECKOUT_SESSION_ID}";
                
            String cancelUrl = request.getCancelUrl() != null ? 
                request.getCancelUrl() : 
                frontendUrl + "/customer/checkout?status=cancelled";
            
            // Crear sesión de checkout
            SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .setCustomerEmail(request.getCustomerEmail())
                .addAllLineItem(lineItems)
                .putAllMetadata(metadata)
                .build();
            
            Session session = Session.create(params);
            
            log.info("Stripe session created successfully: {}", session.getId());
            
            return StripeCheckoutResponse.builder()
                .sessionId(session.getId())
                .checkoutUrl(session.getUrl())
                .status(session.getStatus())
                .paymentIntentId(session.getPaymentIntent())
                .build();
                
        } catch (StripeException e) {
            log.error("Error creating Stripe checkout session: {}", e.getMessage(), e);
            throw new PaymentException("Error creating payment session: " + e.getMessage());
        }
    }
    
    public Session getSession(String sessionId) {
        try {
            return Session.retrieve(sessionId);
        } catch (StripeException e) {
            log.error("Error retrieving Stripe session: {}", e.getMessage(), e);
            throw new PaymentException("Error retrieving session: " + e.getMessage());
        }
    }
    
    public Session retrieveSession(String sessionId) {
        try {
            return Session.retrieve(sessionId);
        } catch (StripeException e) {
            log.error("Error retrieving Stripe session: {}", e.getMessage(), e);
            throw new PaymentException("Error retrieving session: " + e.getMessage());
        }
    }
    
    /**
     * Obtiene la URL de checkout de una sesión existente
     */
    public String getCheckoutUrlFromSession(String sessionId) {
        try {
            Session session = Session.retrieve(sessionId);
            return session.getUrl();
        } catch (StripeException e) {
            log.error("Error retrieving checkout URL from session: {}", e.getMessage(), e);
            throw new PaymentException("Error retrieving checkout URL: " + e.getMessage());
        }
    }
    
    private Long convertToCents(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).longValue();
    }
    
    public String getWebhookSecret() {
        return webhookSecret;
    }
}

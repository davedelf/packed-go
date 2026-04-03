package com.packedgo.payment_service.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.JsonSyntaxException;
import com.packedgo.payment_service.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final PaymentService paymentService;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    /**
     * Endpoint para recibir webhooks de Stripe
     */
    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        log.info("🔷 Webhook recibido de Stripe");

        if (sigHeader == null) {
            log.error("❌ Falta header Stripe-Signature");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing Stripe-Signature header");
        }

        Event event;

        try {
            // Verificar firma del webhook
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            log.info("✅ Firma de webhook verificada. Tipo de evento: {}", event.getType());

        } catch (JsonSyntaxException e) {
            log.error("❌ Error de sintaxis JSON: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid JSON");
        } catch (SignatureVerificationException e) {
            log.error("❌ Verificación de firma falló: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        // Manejar el evento
        if ("checkout.session.completed".equals(event.getType())) {
            handleCheckoutSessionCompleted(event);
        } else {
            log.info("ℹ️ Evento ignorado: {}", event.getType());
        }

        return ResponseEntity.ok("Webhook processed");
    }

    /**
     * Maneja el evento checkout.session.completed
     */
    private void handleCheckoutSessionCompleted(Event event) {
        log.info("🔷 Procesando evento checkout.session.completed");

        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = null;

        if (dataObjectDeserializer.getObject().isPresent()) {
            stripeObject = dataObjectDeserializer.getObject().get();
        } else {
            log.error("❌ No se pudo deserializar el objeto del evento");
            return;
        }

        if (stripeObject instanceof Session session) {
            String sessionId = session.getId();
            log.info("✅ Sesión completada: {}", sessionId);

            try {
                // Llamar al servicio para marcar el pago como aprobado
                paymentService.handleStripePaymentSuccess(sessionId);
                log.info("✅ Pago procesado exitosamente");
            } catch (Exception e) {
                log.error("❌ Error procesando pago: {}", e.getMessage(), e);
            }
        } else {
            log.warn("⚠️ Objeto del evento no es una Session");
        }
    }
}

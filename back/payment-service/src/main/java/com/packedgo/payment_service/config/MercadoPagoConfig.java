package com.packedgo.payment_service.config;

import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * Configuración de MercadoPago
 * IMPORTANTE: No configuramos el access token aquí de forma estática
 * Cada operación de pago usará el access token del admin correspondiente
 */
@Configuration
@Slf4j
public class MercadoPagoConfig {
    
    public MercadoPagoConfig() {
        log.info("MercadoPago Configuration initialized - Dynamic token per admin");
    }
}

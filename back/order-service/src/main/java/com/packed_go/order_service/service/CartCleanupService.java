package com.packed_go.order_service.service;

import com.packed_go.order_service.entity.ShoppingCart;
import com.packed_go.order_service.repository.ShoppingCartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio para limpiar automáticamente carritos expirados
 * Se ejecuta cada 5 minutos (configurable)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartCleanupService {
    
    private final ShoppingCartRepository cartRepository;
    
    /**
     * Tarea programada para marcar carritos expirados
     * Cron: cada 5 minutos
     * - Se ejecuta en: 00:00, 00:05, 00:10, ..., 23:55
     */
    @Scheduled(cron = "0 */5 * * * *") // Cada 5 minutos
    @Transactional
    public void markExpiredCarts() {
        log.info("Starting scheduled cart cleanup task");
        
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // Buscar carritos activos que ya expiraron
            List<ShoppingCart> expiredCarts = cartRepository
                    .findByStatusAndExpiresAtBefore("ACTIVE", now);
            
            if (expiredCarts.isEmpty()) {
                log.info("No expired carts found");
                return;
            }
            
            log.info("Found {} expired carts", expiredCarts.size());
            
            // Marcar cada carrito como expirado
            for (ShoppingCart cart : expiredCarts) {
                cart.markAsExpired();
                log.debug("Cart {} marked as expired for user {}", cart.getId(), cart.getUserId());
            }
            
            // Guardar cambios
            cartRepository.saveAll(expiredCarts);
            
            log.info("Successfully marked {} carts as expired", expiredCarts.size());
            
        } catch (Exception e) {
            log.error("Error during cart cleanup task: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Tarea programada para eliminar carritos muy antiguos
     * Cron: cada día a las 3:00 AM
     * Elimina carritos con status EXPIRED o COMPLETED de más de 30 días
     */
    @Scheduled(cron = "0 0 3 * * *") // Diario a las 3 AM
    @Transactional
    public void deleteOldCarts() {
        log.info("Starting old cart deletion task");
        
        try {
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            
            // Eliminar carritos expirados de hace más de 30 días
            cartRepository.deleteByStatusAndCreatedAtBefore("EXPIRED", thirtyDaysAgo);
            log.info("Deleted old EXPIRED carts from before {}", thirtyDaysAgo);
            
            // Eliminar carritos completados de hace más de 30 días
            cartRepository.deleteByStatusAndCreatedAtBefore("COMPLETED", thirtyDaysAgo);
            log.info("Deleted old COMPLETED carts from before {}", thirtyDaysAgo);
            
            log.info("Old cart deletion task completed");
            
        } catch (Exception e) {
            log.error("Error during old cart deletion task: {}", e.getMessage(), e);
        }
    }
}

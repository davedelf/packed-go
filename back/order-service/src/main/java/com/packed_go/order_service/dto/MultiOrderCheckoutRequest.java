package com.packed_go.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear una sesión de múltiples órdenes desde un carrito
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultiOrderCheckoutRequest {
    private Long cartId;
    private Long userId;
}

package com.packed_go.order_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.packed_go.order_service.dto.request.AddConsumptionToItemRequest;
import com.packed_go.order_service.dto.request.AddToCartRequest;
import com.packed_go.order_service.dto.request.UpdateCartItemRequest;
import com.packed_go.order_service.dto.response.CartDTO;
import com.packed_go.order_service.security.JwtTokenValidator;
import com.packed_go.order_service.service.CartService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {
    
    private final CartService cartService;
    private final JwtTokenValidator jwtTokenValidator;
    
    /**
     * Agregar un evento con consumos al carrito
     * 
     * POST /api/cart/add
     * Headers: Authorization: Bearer {token}
     * Body: { "eventId": 1, "consumptions": [{ "consumptionId": 1, "quantity": 2 }] }
     * 
     * @return 201 CREATED con el carrito actualizado
     */
    @PostMapping("/add")
    public ResponseEntity<CartDTO> addToCart(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody AddToCartRequest request) {
        
        log.info("POST /api/cart/add - Adding event {} to cart", request.getEventId());
        
        Long userId = extractUserId(authHeader);
        CartDTO cart = cartService.addToCart(userId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(cart);
    }
    
    /**
     * Obtener el carrito activo del usuario
     * 
     * GET /api/cart
     * Headers: Authorization: Bearer {token}
     * 
     * @return 200 OK con el carrito o 404 si no existe
     */
    @GetMapping
    public ResponseEntity<CartDTO> getCart(
            @RequestHeader("Authorization") String authHeader) {
        
        log.info("GET /api/cart - Retrieving cart");
        
        Long userId = extractUserId(authHeader);
        CartDTO cart = cartService.getActiveCart(userId);
        
        return ResponseEntity.ok(cart);
    }
    
    /**
     * Eliminar un item del carrito
     * 
     * DELETE /api/cart/items/{itemId}
     * Headers: Authorization: Bearer {token}
     * 
     * @return 200 OK con carrito actualizado o 204 NO CONTENT si el carrito quedó vacío
     */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartDTO> removeCartItem(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long itemId) {
        
        log.info("DELETE /api/cart/items/{} - Removing item from cart", itemId);
        
        Long userId = extractUserId(authHeader);
        CartDTO cart = cartService.removeCartItem(userId, itemId);
        
        if (cart == null) {
            return ResponseEntity.noContent().build();
        }
        
        return ResponseEntity.ok(cart);
    }
    
    /**
     * Vaciar completamente el carrito
     * 
     * DELETE /api/cart
     * Headers: Authorization: Bearer {token}
     * 
     * @return 204 NO CONTENT
     */
    @DeleteMapping
    public ResponseEntity<Void> clearCart(
            @RequestHeader("Authorization") String authHeader) {
        
        log.info("DELETE /api/cart - Clearing cart");
        
        Long userId = extractUserId(authHeader);
        cartService.clearCart(userId);
        
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Actualizar la cantidad de un item en el carrito
     * 
     * PUT /api/cart/items/{itemId}
     * Headers: Authorization: Bearer {token}
     * Body: { "quantity": 3 }
     * 
     * @return 200 OK con el carrito actualizado
     */
    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartDTO> updateCartItemQuantity(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        
        log.info("PUT /api/cart/items/{} - Updating item quantity to {}", itemId, request.getQuantity());
        
        Long userId = extractUserId(authHeader);
        CartDTO cart = cartService.updateCartItemQuantity(userId, itemId, request);
        
        return ResponseEntity.ok(cart);
    }
    
    /**
     * Actualizar la cantidad de una consumición dentro de un item
     * 
     * PUT /api/cart/items/{itemId}/consumptions/{consumptionId}
     * Headers: Authorization: Bearer {token}
     * Body: { "quantity": 2 }
     * 
     * @return 200 OK con el carrito actualizado
     */
    @PutMapping("/items/{itemId}/consumptions/{consumptionId}")
    public ResponseEntity<CartDTO> updateConsumptionQuantity(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long itemId,
            @PathVariable Long consumptionId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        
        log.info("PUT /api/cart/items/{}/consumptions/{} - Updating consumption quantity to {}", 
                itemId, consumptionId, request.getQuantity());
        
        Long userId = extractUserId(authHeader);
        CartDTO cart = cartService.updateConsumptionQuantity(userId, itemId, consumptionId, request.getQuantity());
        
        return ResponseEntity.ok(cart);
    }
    
    /**
     * Agregar una nueva consumición a un item existente del carrito
     * 
     * POST /api/cart/items/{itemId}/consumptions
     * Headers: Authorization: Bearer {token}
     * Body: { "consumptionId": 2, "quantity": 1 }
     * 
     * @return 200 OK con el carrito actualizado
     */
    @PostMapping("/items/{itemId}/consumptions")
    public ResponseEntity<CartDTO> addConsumptionToItem(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long itemId,
            @Valid @RequestBody AddConsumptionToItemRequest request) {
        
        log.info("POST /api/cart/items/{}/consumptions - Adding consumption {} with quantity {}", 
                itemId, request.getConsumptionId(), request.getQuantity());
        
        Long userId = extractUserId(authHeader);
        CartDTO cart = cartService.addConsumptionToItem(userId, itemId, request);
        
        return ResponseEntity.ok(cart);
    }
    
    /**
     * Eliminar una consumición de un item del carrito
     * 
     * DELETE /api/cart/items/{itemId}/consumptions/{consumptionId}
     * Headers: Authorization: Bearer {token}
     * 
     * @return 200 OK con el carrito actualizado
     */
    @DeleteMapping("/items/{itemId}/consumptions/{consumptionId}")
    public ResponseEntity<CartDTO> removeConsumptionFromItem(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long itemId,
            @PathVariable Long consumptionId) {
        
        log.info("DELETE /api/cart/items/{}/consumptions/{} - Removing consumption", itemId, consumptionId);
        
        Long userId = extractUserId(authHeader);
        CartDTO cart = cartService.removeConsumptionFromItem(userId, itemId, consumptionId);
        
        return ResponseEntity.ok(cart);
    }
    
    /**
     * Extrae el userId del token JWT
     */
    private Long extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        
        if (!jwtTokenValidator.validateToken(token)) {
            throw new RuntimeException("Invalid token");
        }
        
        return jwtTokenValidator.getUserIdFromToken(token);
    }
}

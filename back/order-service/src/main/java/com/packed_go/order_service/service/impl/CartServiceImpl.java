package com.packed_go.order_service.service.impl;

import com.packed_go.order_service.dto.external.ConsumptionDTO;
import com.packed_go.order_service.dto.external.EventDTO;
import com.packed_go.order_service.dto.request.AddToCartRequest;
import com.packed_go.order_service.dto.request.UpdateCartItemRequest;
import com.packed_go.order_service.dto.response.CartDTO;
import com.packed_go.order_service.dto.response.CartItemConsumptionDTO;
import com.packed_go.order_service.dto.response.CartItemDTO;
import com.packed_go.order_service.entity.CartItem;
import com.packed_go.order_service.entity.CartItemConsumption;
import com.packed_go.order_service.entity.ShoppingCart;
import com.packed_go.order_service.exception.CartExpiredException;
import com.packed_go.order_service.exception.CartNotFoundException;
import com.packed_go.order_service.exception.StockNotAvailableException;
import com.packed_go.order_service.external.EventServiceClient;
import com.packed_go.order_service.repository.CartItemRepository;
import com.packed_go.order_service.repository.ShoppingCartRepository;
import com.packed_go.order_service.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    
    // Constantes de límites
    private static final int MAX_TICKETS_PER_PERSON = 10;
    /**
     * Límite máximo TOTAL de consumiciones por entrada individual
     * (suma de TODAS las consumiciones de una entrada)
     */
    private static final int MAX_TOTAL_CONSUMPTIONS_PER_TICKET = 20;
    
    private final ShoppingCartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final EventServiceClient eventServiceClient;
    private final ModelMapper modelMapper;
    
    @Override
    @Transactional
    public CartDTO addToCart(Long userId, AddToCartRequest request) {
        log.info("Adding event {} to cart for user {}", request.getEventId(), userId);
        
        // 1. Validar que el evento existe y tiene stock disponible
        EventDTO event = eventServiceClient.getEventById(request.getEventId());
        if (!eventServiceClient.checkPassAvailability(request.getEventId())) {
            log.warn("Event {} has no available passes", request.getEventId());
            throw new StockNotAvailableException(request.getEventId());
        }
        
        // 2. Obtener o crear carrito activo para el usuario (con items cargados)
        // Toma el más reciente si hay múltiples (por updatedAt DESC)
        List<ShoppingCart> activeCarts = cartRepository.findByUserIdAndStatusWithItems(userId, "ACTIVE");
        ShoppingCart cart = activeCarts.isEmpty() ? null : activeCarts.get(0);
        
        // 3. Verificar si el carrito existe y si expiró
        if (cart != null && cart.isExpired()) {
            log.warn("Cart {} expired for user {}, creating new cart", cart.getId(), userId);
            cart.markAsExpired();
            cartRepository.save(cart);
            // Crear un nuevo carrito después de marcar el anterior como expirado
            cart = createNewCart(userId);
        } else if (cart == null) {
            // No hay carrito activo, crear uno nuevo
            log.info("No active cart found for user {}, creating new cart", userId);
            cart = createNewCart(userId);
        }
        
        // 4. Obtener cantidad a agregar (default 1 si no viene en el request)
        int quantityToAdd = (request.getQuantity() != null && request.getQuantity() > 0) 
                ? request.getQuantity() 
                : 1;
        
        // 5. Validar límite total de entradas en el carrito para este evento
        int totalEventTicketsInCart = cart.getItems().stream()
                .filter(item -> item.getEventId().equals(request.getEventId()))
                .mapToInt(CartItem::getQuantity)
                .sum();
        
        log.info("Event {} - Current tickets in cart: {}, Adding: {}, Total would be: {}", 
                request.getEventId(), totalEventTicketsInCart, quantityToAdd, totalEventTicketsInCart + quantityToAdd);
        
        int newTotalQuantity = totalEventTicketsInCart + quantityToAdd;
        
        // Validar que no exceda el máximo permitido
        if (newTotalQuantity > MAX_TICKETS_PER_PERSON) {
            log.warn("Limit exceeded for event {}: trying to add {} when already have {}", 
                    request.getEventId(), quantityToAdd, totalEventTicketsInCart);
            throw new IllegalArgumentException(
                String.format("Cannot add more than %d tickets per person for this event (current: %d, adding: %d)", 
                    MAX_TICKETS_PER_PERSON, totalEventTicketsInCart, quantityToAdd)
            );
        }
        
        // 6. OPCIÓN A: Crear N items separados, cada uno con quantity=1
        // Esto permite que cada entrada sea independiente y tenga su propio QR
        // Usuario verá exactamente cuántos QR recibirá (1 QR por item)
        log.info("Creating {} separate cart items (quantity=1 each) for event {}", quantityToAdd, request.getEventId());
        
        for (int i = 0; i < quantityToAdd; i++) {
            // Crear un CartItem individual con quantity=1
            CartItem cartItem = CartItem.builder()
                    .cart(cart)
                    .eventId(request.getEventId())
                    .eventName(event.getName())
                    .adminId(event.getCreatedBy()) // IMPORTANTE: AdminId para multitenant
                    .quantity(1)  // SIEMPRE 1 - cada item = 1 entrada = 1 QR
                    .unitPrice(event.getBasePrice())
                    .consumptions(new ArrayList<>())
                    .build();
            
            // 7. Agregar consumos al item (si hay)
            if (request.getConsumptions() != null && !request.getConsumptions().isEmpty()) {
                List<ConsumptionDTO> consumptionsInfo = eventServiceClient.getEventConsumptions(request.getEventId());
                
                // Calcular total de consumiciones para este item
                int totalConsumptions = request.getConsumptions().stream()
                        .mapToInt(AddToCartRequest.ConsumptionRequest::getQuantity)
                        .sum();
                
                // Validar límite TOTAL de consumiciones (no por consumición individual, sino la suma total)
                if (totalConsumptions > MAX_TOTAL_CONSUMPTIONS_PER_TICKET) {
                    log.warn("Cannot add consumptions: total quantity {} exceeds limit {}", 
                            totalConsumptions, MAX_TOTAL_CONSUMPTIONS_PER_TICKET);
                    throw new IllegalArgumentException(
                        String.format("Cannot add more than %d consumptions in total per ticket (requested total: %d)", 
                            MAX_TOTAL_CONSUMPTIONS_PER_TICKET, totalConsumptions)
                    );
                }
                
                for (AddToCartRequest.ConsumptionRequest consumptionReq : request.getConsumptions()) {
                    ConsumptionDTO consumptionInfo = consumptionsInfo.stream()
                            .filter(c -> c.getId().equals(consumptionReq.getConsumptionId()))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Consumption " + consumptionReq.getConsumptionId() + " not found"));
                    
                    // Crear consumo para este item específico
                    CartItemConsumption newConsumption = CartItemConsumption.builder()
                            .cartItem(cartItem)
                            .consumptionId(consumptionReq.getConsumptionId())
                            .consumptionName(consumptionInfo.getName())
                            .quantity(consumptionReq.getQuantity())
                            .unitPrice(consumptionInfo.getPrice())
                            .build();
                    
                    // Calcular subtotal de la consumición
                    newConsumption.updateQuantity(consumptionReq.getQuantity());
                    
                    cartItem.getConsumptions().add(newConsumption);
                }
            }
            
            // 8. Calcular subtotal del item (entrada + consumiciones)
            cartItem.calculateSubtotal();
            
            // 9. Agregar item al carrito
            cart.getItems().add(cartItem);
        }
        
        log.info("Created {} cart items for event {}", quantityToAdd, request.getEventId());
        
        // 10. Guardar carrito (esto persiste todos los CartItems con sus subtotales calculados)
        ShoppingCart savedCart = cartRepository.save(cart);
        log.info("{} items for event {} added to cart {} successfully", quantityToAdd, request.getEventId(), savedCart.getId());
        
        return convertToDTO(savedCart);
    }
    
    @Override
    @Transactional
    public CartDTO getActiveCart(Long userId) {
        log.info("Retrieving active cart for user {}", userId);
        
        List<ShoppingCart> activeCarts = cartRepository.findByUserIdAndStatusWithItems(userId, "ACTIVE");
        ShoppingCart cart = activeCarts.isEmpty() ? null : activeCarts.get(0);
        
        // Si no hay carrito o expiró, crear uno nuevo
        if (cart == null) {
            log.info("No active cart found for user {}, creating new cart", userId);
            cart = createNewCart(userId);
        } else if (cart.isExpired()) {
            log.info("Cart {} expired for user {}, marking as expired and creating new cart", cart.getId(), userId);
            cart.setStatus("EXPIRED");
            cartRepository.save(cart);
            cart = createNewCart(userId);
        }
        
        return convertToDTO(cart);
    }
    
    @Override
    @Transactional
    public CartDTO removeCartItem(Long userId, Long itemId) {
        log.info("Removing item {} from cart for user {}", itemId, userId);
        
        List<ShoppingCart> activeCarts = cartRepository.findByUserIdAndStatusWithItems(userId, "ACTIVE");
        if (activeCarts.isEmpty()) {
            throw new CartNotFoundException(userId);
        }
        ShoppingCart cart = activeCarts.get(0);
        
        // Verificar si expiró
        if (cart.isExpired()) {
            log.warn("Cart {} expired for user {}", cart.getId(), userId);
            cart.markAsExpired();
            cartRepository.save(cart);
            throw new CartExpiredException(cart.getId());
        }
        
        // Eliminar el item
        boolean removed = cart.getItems().removeIf(item -> item.getId().equals(itemId));
        
        if (!removed) {
            log.warn("Item {} not found in cart {}", itemId, cart.getId());
            throw new RuntimeException("Item not found in cart");
        }
        
        // Si el carrito quedó vacío, eliminarlo
        if (cart.getItems().isEmpty()) {
            log.info("Cart {} is now empty, deleting it", cart.getId());
            cartRepository.delete(cart);
            return null;
        }
        
        ShoppingCart savedCart = cartRepository.save(cart);
        log.info("Item {} removed from cart {} successfully", itemId, savedCart.getId());
        
        return convertToDTO(savedCart);
    }
    
    @Override
    @Transactional
    public void clearCart(Long userId) {
        log.info("Clearing cart for user {}", userId);
        
        List<ShoppingCart> activeCarts = cartRepository.findByUserIdAndStatusWithItems(userId, "ACTIVE");
        if (activeCarts.isEmpty()) {
            throw new CartNotFoundException(userId);
        }
        ShoppingCart cart = activeCarts.get(0);
        
        cartRepository.delete(cart);
        log.info("Cart {} cleared successfully", cart.getId());
    }
    
    @Override
    @Transactional
    public CartDTO updateCartItemQuantity(Long userId, Long itemId, UpdateCartItemRequest request) {
        log.info("Updating item {} quantity to {} for user {}", itemId, request.getQuantity(), userId);
        
        // OPCIÓN A: Cada item tiene quantity=1 siempre
        // Este método ya NO se usa para incrementar/decrementar
        // Los botones +/- del frontend ahora crean/eliminan items completos
        if (request.getQuantity() != 1) {
            throw new IllegalArgumentException("Quantity must be 1. Use add/remove methods to manage cart items.");
        }
        
        List<ShoppingCart> activeCarts = cartRepository.findByUserIdAndStatusWithItems(userId, "ACTIVE");
        if (activeCarts.isEmpty()) {
            throw new CartNotFoundException(userId);
        }
        ShoppingCart cart = activeCarts.get(0);
        
        // Verificar si expiró
        if (cart.isExpired()) {
            log.warn("Cart {} expired for user {}", cart.getId(), userId);
            cart.markAsExpired();
            cartRepository.save(cart);
            throw new CartExpiredException(cart.getId());
        }
        
        // Buscar el item
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));
        
        // No hacer nada, quantity ya es 1
        log.info("Item {} already has quantity=1, no changes needed", itemId);
        
        ShoppingCart savedCart = cartRepository.save(cart);
        log.info("Item {} quantity updated successfully", itemId);
        
        return convertToDTO(savedCart);
    }
    
    @Override
    @Transactional
    public CartDTO updateConsumptionQuantity(Long userId, Long itemId, Long consumptionId, Integer newQuantity) {
        log.info("Updating consumption {} quantity to {} in item {} for user {}", 
                consumptionId, newQuantity, itemId, userId);
        
        List<ShoppingCart> activeCarts = cartRepository.findByUserIdAndStatusWithItems(userId, "ACTIVE");
        if (activeCarts.isEmpty()) {
            throw new CartNotFoundException(userId);
        }
        ShoppingCart cart = activeCarts.get(0);
        
        // Verificar si expiró
        if (cart.isExpired()) {
            log.warn("Cart {} expired for user {}", cart.getId(), userId);
            cart.markAsExpired();
            cartRepository.save(cart);
            throw new CartExpiredException(cart.getId());
        }
        
        // Buscar el item
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));
        
        // Buscar la consumición
        CartItemConsumption consumption = item.getConsumptions().stream()
                .filter(c -> c.getConsumptionId().equals(consumptionId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Consumption not found in item"));
        
        // Calcular el total de consumiciones que tendría el item después de este cambio
        int totalConsumptionsAfterUpdate = item.getConsumptions().stream()
                .mapToInt(c -> c.getConsumptionId().equals(consumptionId) ? newQuantity : c.getQuantity())
                .sum();
        
        // Validar límite TOTAL de consumiciones (suma de todas)
        if (totalConsumptionsAfterUpdate > MAX_TOTAL_CONSUMPTIONS_PER_TICKET) {
            log.warn("Attempted to set total consumptions to {} (max: {})", totalConsumptionsAfterUpdate, MAX_TOTAL_CONSUMPTIONS_PER_TICKET);
            throw new IllegalArgumentException(
                String.format("Cannot exceed %d total consumptions per ticket (would result in: %d)", 
                    MAX_TOTAL_CONSUMPTIONS_PER_TICKET, totalConsumptionsAfterUpdate)
            );
        }
        
        // Actualizar cantidad de la consumición
        consumption.updateQuantity(newQuantity);
        
        // Recalcular subtotal del item
        item.calculateSubtotal();
        
        ShoppingCart savedCart = cartRepository.save(cart);
        log.info("Consumption {} quantity updated successfully", consumptionId);
        
        return convertToDTO(savedCart);
    }
    
    @Override
    @Transactional
    public CartDTO addConsumptionToItem(Long userId, Long itemId, 
                                        com.packed_go.order_service.dto.request.AddConsumptionToItemRequest request) {
        log.info("Adding consumption {} (quantity: {}) to item {} for user {}", 
                request.getConsumptionId(), request.getQuantity(), itemId, userId);
        
        // 1. Obtener carrito activo
        List<ShoppingCart> activeCarts = cartRepository.findByUserIdAndStatusWithItems(userId, "ACTIVE");
        if (activeCarts.isEmpty()) {
            throw new CartNotFoundException(userId);
        }
        ShoppingCart cart = activeCarts.get(0);
        
        // Verificar si expiró
        if (cart.isExpired()) {
            log.warn("Cart {} expired for user {}", cart.getId(), userId);
            cart.markAsExpired();
            cartRepository.save(cart);
            throw new CartExpiredException(cart.getId());
        }
        
        // 2. Buscar el item en el carrito
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));
        
        // 3. Obtener información de la consumición desde event-service
        List<ConsumptionDTO> consumptions = eventServiceClient.getEventConsumptions(item.getEventId());
        ConsumptionDTO consumptionInfo = consumptions.stream()
                .filter(c -> c.getId().equals(request.getConsumptionId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Consumption not found in event"));
        
        // 4. Verificar si la consumición ya existe en el item
        CartItemConsumption existingConsumption = item.getConsumptions().stream()
                .filter(c -> c.getConsumptionId().equals(request.getConsumptionId()))
                .findFirst()
                .orElse(null);
        
        if (existingConsumption != null) {
            // Si ya existe, incrementar la cantidad
            log.info("Consumption {} already exists in item, incrementing quantity", request.getConsumptionId());
            int newQuantity = existingConsumption.getQuantity() + request.getQuantity();
            
            // Calcular el total de consumiciones después de agregar
            int totalConsumptionsAfterAdd = item.getConsumptions().stream()
                    .mapToInt(c -> c.getConsumptionId().equals(request.getConsumptionId()) ? newQuantity : c.getQuantity())
                    .sum();
            
            // Validar límite TOTAL de consumiciones
            if (totalConsumptionsAfterAdd > MAX_TOTAL_CONSUMPTIONS_PER_TICKET) {
                log.warn("Cannot add consumption: would exceed total limit (would result in: {}, max: {})", 
                        totalConsumptionsAfterAdd, MAX_TOTAL_CONSUMPTIONS_PER_TICKET);
                throw new IllegalArgumentException(
                    String.format("Cannot exceed %d total consumptions per ticket (would result in: %d)", 
                        MAX_TOTAL_CONSUMPTIONS_PER_TICKET, totalConsumptionsAfterAdd)
                );
            }
            
            existingConsumption.updateQuantity(newQuantity);
        } else {
            // Si no existe, crear una nueva
            log.info("Creating new consumption {} in item", request.getConsumptionId());
            
            // Calcular el total de consumiciones después de agregar esta nueva
            int totalConsumptionsAfterAdd = item.getConsumptions().stream()
                    .mapToInt(CartItemConsumption::getQuantity)
                    .sum() + request.getQuantity();
            
            // Validar límite TOTAL de consumiciones
            if (totalConsumptionsAfterAdd > MAX_TOTAL_CONSUMPTIONS_PER_TICKET) {
                log.warn("Cannot add consumption: would exceed total limit (would result in: {}, max: {})", 
                        totalConsumptionsAfterAdd, MAX_TOTAL_CONSUMPTIONS_PER_TICKET);
                throw new IllegalArgumentException(
                    String.format("Cannot exceed %d total consumptions per ticket (would result in: %d)", 
                        MAX_TOTAL_CONSUMPTIONS_PER_TICKET, totalConsumptionsAfterAdd)
                );
            }
            
            CartItemConsumption newConsumption = CartItemConsumption.builder()
                    .cartItem(item)
                    .consumptionId(consumptionInfo.getId())
                    .consumptionName(consumptionInfo.getName())
                    .quantity(1) // Inicializar con 1
                    .unitPrice(consumptionInfo.getPrice())
                    .build();
            
            // Actualizar cantidad (esto calcula el subtotal automáticamente)
            newConsumption.updateQuantity(request.getQuantity());
            
            // Agregar al item
            item.getConsumptions().add(newConsumption);
        }
        
        // 5. Recalcular subtotal del item
        item.calculateSubtotal();
        
        // 6. Guardar y retornar
        ShoppingCart savedCart = cartRepository.save(cart);
        log.info("Consumption {} added successfully to item {}", request.getConsumptionId(), itemId);
        
        return convertToDTO(savedCart);
    }
    
    @Override
    @Transactional
    public CartDTO removeConsumptionFromItem(Long userId, Long itemId, Long consumptionId) {
        log.info("Removing consumption {} from item {} for user {}", consumptionId, itemId, userId);
        
        // 1. Obtener carrito activo
        List<ShoppingCart> activeCarts = cartRepository.findByUserIdAndStatusWithItems(userId, "ACTIVE");
        if (activeCarts.isEmpty()) {
            throw new CartNotFoundException(userId);
        }
        ShoppingCart cart = activeCarts.get(0);
        
        // Verificar si expiró
        if (cart.isExpired()) {
            log.warn("Cart {} expired for user {}", cart.getId(), userId);
            cart.markAsExpired();
            cartRepository.save(cart);
            throw new CartExpiredException(cart.getId());
        }
        
        // 2. Buscar el item en el carrito
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));
        
        // 3. Buscar la consumición en el item
        CartItemConsumption consumptionToRemove = item.getConsumptions().stream()
                .filter(c -> c.getConsumptionId().equals(consumptionId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Consumption not found in item"));
        
        // 4. Eliminar la consumición
        item.removeConsumption(consumptionToRemove);
        log.info("Consumption {} removed from item {}", consumptionId, itemId);
        
        // 5. Recalcular subtotal del item
        item.calculateSubtotal();
        
        // 6. Guardar y retornar
        ShoppingCart savedCart = cartRepository.save(cart);
        log.info("Cart updated after removing consumption {}", consumptionId);
        
        return convertToDTO(savedCart);
    }
    
    /**
     * Crea un nuevo carrito para el usuario
     */
    private ShoppingCart createNewCart(Long userId) {
        log.info("Creating new cart for user {}", userId);
        
        ShoppingCart cart = ShoppingCart.builder()
                .userId(userId)
                .status("ACTIVE")
                .items(new ArrayList<>())
                .build();
        
        return cartRepository.save(cart);
    }
    
    /**
     * Convierte una entidad ShoppingCart a CartDTO
     */
    private CartDTO convertToDTO(ShoppingCart cart) {
        // Ordenar items por ID para mantener orden consistente
        List<CartItemDTO> sortedItems = cart.getItems().stream()
                .sorted((i1, i2) -> i1.getId().compareTo(i2.getId()))
                .map(this::convertItemToDTO)
                .collect(Collectors.toList());
        
        CartDTO dto = CartDTO.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .status(cart.getStatus())
                .expiresAt(cart.getExpiresAt())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .expired(cart.isExpired())
                .totalAmount(cart.getTotalAmount())
                .itemCount(cart.getItems().size())
                .items(sortedItems)
                .build();
        
        return dto;
    }
    
    /**
     * Convierte un CartItem a CartItemDTO
     */
    private CartItemDTO convertItemToDTO(CartItem item) {
        // Ordenar consumiciones por ID para mantener orden consistente
        List<CartItemConsumptionDTO> sortedConsumptions = item.getConsumptions().stream()
                .sorted((c1, c2) -> c1.getConsumptionId().compareTo(c2.getConsumptionId()))
                .map(this::convertConsumptionToDTO)
                .collect(Collectors.toList());
        
        return CartItemDTO.builder()
                .id(item.getId())
                .eventId(item.getEventId())
                .eventName(item.getEventName())
                .adminId(item.getAdminId())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .consumptions(sortedConsumptions)
                .build();
    }
    
    /**
     * Convierte un CartItemConsumption a CartItemConsumptionDTO
     */
    private CartItemConsumptionDTO convertConsumptionToDTO(CartItemConsumption consumption) {
        return CartItemConsumptionDTO.builder()
                .id(consumption.getId())
                .consumptionId(consumption.getConsumptionId())
                .consumptionName(consumption.getConsumptionName())
                .quantity(consumption.getQuantity())
                .unitPrice(consumption.getUnitPrice())
                .subtotal(consumption.getSubtotal())
                .build();
    }
}

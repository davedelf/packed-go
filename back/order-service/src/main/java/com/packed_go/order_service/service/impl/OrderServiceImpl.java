package com.packed_go.order_service.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.packed_go.order_service.client.PaymentServiceClient;
import com.packed_go.order_service.dto.external.CreateTicketWithConsumptionsRequest;
import com.packed_go.order_service.dto.external.PaymentServiceRequest;
import com.packed_go.order_service.dto.external.PaymentServiceResponse;
import com.packed_go.order_service.dto.external.TicketConsumptionDTO;
import com.packed_go.order_service.dto.external.TicketWithConsumptionsResponse;
import com.packed_go.order_service.dto.request.CheckoutRequest;
import com.packed_go.order_service.dto.request.PaymentCallbackRequest;
import com.packed_go.order_service.dto.response.CheckoutResponse;
import com.packed_go.order_service.entity.CartItem;
import com.packed_go.order_service.entity.Order;
import com.packed_go.order_service.entity.OrderItem;
import com.packed_go.order_service.entity.ShoppingCart;
import com.packed_go.order_service.exception.CartExpiredException;
import com.packed_go.order_service.exception.CartNotFoundException;
import com.packed_go.order_service.external.EventServiceClient;
import com.packed_go.order_service.repository.OrderRepository;
import com.packed_go.order_service.repository.ShoppingCartRepository;
import com.packed_go.order_service.service.EmailService;
import com.packed_go.order_service.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {
    
    private final OrderRepository orderRepository;
    private final ShoppingCartRepository cartRepository;
    private final PaymentServiceClient paymentServiceClient;
    private final EventServiceClient eventServiceClient;
    private final EmailService emailService;
    
    @Override
    @Transactional
    public CheckoutResponse checkout(Long userId, CheckoutRequest request) {
        // Deprecated single-order checkout
        return null;
    }

    @Override
    @Transactional
    public CheckoutResponse checkoutSingleAdmin(Long userId, Long adminId, String timezone) {
        log.info("Processing SINGLE ADMIN checkout for user: {} and admin: {} with timezone: {}", userId, adminId, timezone);

        // 1. Obtener carrito activo
        List<ShoppingCart> activeCarts = cartRepository.findByUserIdAndStatusWithItems(userId, "ACTIVE");
        if (activeCarts.isEmpty()) {
            throw new CartNotFoundException("No active cart found for user");
        }
        ShoppingCart cart = activeCarts.get(0);

        if (cart.isExpired()) {
            cart.markAsExpired();
            cartRepository.save(cart);
            throw new CartExpiredException("Cart has expired");
        }

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        // 2. Filtrar items del admin seleccionado
        List<CartItem> adminItems = cart.getItems().stream()
                .filter(item -> item.getAdminId().equals(adminId))
                .collect(Collectors.toList());

        if (adminItems.isEmpty()) {
            throw new IllegalStateException("No items found for admin " + adminId + " in the cart");
        }

        // 3. Crear Orden Única
        Order order = createOrderFromCartItems(cart, adminId, adminItems, null, timezone); // sessionId es null, timezone del cliente
        order = orderRepository.save(order);
        
        log.info("Created Order {} for Admin {}", order.getOrderNumber(), adminId);

        // 4. Crear Pago en Stripe
        try {
            PaymentServiceRequest paymentRequest = PaymentServiceRequest.builder()
                    .adminId(adminId)
                    .orderId(order.getOrderNumber())
                    .amount(order.getTotalAmount())
                    .description("Orden " + order.getOrderNumber() + " - PackedGo Events")
                    // URLs simples, sin sessionId
                    .successUrl("http://localhost:3000/customer/orders/success?orderId=" + order.getOrderNumber())
                    .failureUrl("http://localhost:3000/customer/dashboard?paymentStatus=failure&orderId=" + order.getOrderNumber())
                    .pendingUrl("http://localhost:3000/customer/dashboard?paymentStatus=pending&orderId=" + order.getOrderNumber())
                    .build();

            PaymentServiceResponse paymentResponse = paymentServiceClient.createPaymentStripe(paymentRequest);

            order.setPaymentId(paymentResponse.getPaymentId());
            order.setPaymentPreferenceId(paymentResponse.getSessionId());
            order.setCheckoutUrl(paymentResponse.getCheckoutUrl());
            orderRepository.save(order);
            
            // Opcional: Podríamos remover los items del carrito aquí, o esperar al webhook de pago exitoso.
            // Por ahora, los dejamos en el carrito. Si el usuario paga, el webhook marcará la orden como PAID.
            // La limpieza del carrito se puede hacer en el webhook o el frontend puede refrescar.

            return CheckoutResponse.builder()
                    .paymentUrl(paymentResponse.getCheckoutUrl())
                    .preferenceId(paymentResponse.getSessionId())
                    .build();

        } catch (Exception e) {
            log.error("Error creating payment for admin {}: {}", adminId, e.getMessage());
            throw new RuntimeException("Error creating payment: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public void updateOrderFromPaymentCallback(PaymentCallbackRequest request) {
        log.info("Updating order {} with payment status: {}", request.getOrderNumber(), request.getPaymentStatus());
        
        Order order = orderRepository.findByOrderNumber(request.getOrderNumber())
                .orElseThrow(() -> new RuntimeException("Order not found: " + request.getOrderNumber()));
        
        // Actualizar estado según el resultado del pago
        switch (request.getPaymentStatus().toUpperCase()) {
            case "APPROVED":
                order.markAsPaid();
                log.info("Order {} marked as PAID", order.getOrderNumber());
                
                // 🎟️ GENERAR TICKETS cuando el pago es aprobado
                try {
                    List<TicketWithConsumptionsResponse> generatedTickets = generateTicketsForOrder(order);
                    
                    // 📧 ENVIAR EMAIL DE CONFIRMACIÓN con los tickets generados
                    if (request.getCustomerEmail() != null && !request.getCustomerEmail().isEmpty()) {
                        emailService.sendOrderConfirmation(order, request.getCustomerEmail(), generatedTickets);
                    } else {
                        log.warn("⚠️ No customer email provided in callback. Skipping email confirmation.");
                    }

                    // 🛒 LIMPIAR CARRITO (Solo los items comprados)
                    if (order.getCartId() != null) {
                        cartRepository.findById(order.getCartId()).ifPresent(cart -> {
                            log.info("🛒 Updating cart {} for user {}", cart.getId(), cart.getUserId());
                            
                            // Eliminar solo los items que están en la orden pagada
                            // Usamos removeIf para eliminar de la colección y que OrphanRemoval haga su trabajo
                            boolean removed = cart.getItems().removeIf(cartItem -> 
                                order.getItems().stream()
                                    .anyMatch(orderItem -> orderItem.getEventId().equals(cartItem.getEventId()))
                            );
                            
                            if (removed) {
                                log.info("✅ Removed purchased items from cart");
                            }

                            // Si el carrito queda vacío, marcarlo como completado
                            if (cart.getItems().isEmpty()) {
                                cart.setStatus("COMPLETED");
                                log.info("🛒 Cart is empty, marking as COMPLETED");
                            } else {
                                log.info("🛒 Cart still has items, keeping as ACTIVE");
                            }
                            
                            cartRepository.save(cart);
                        });
                    }

                } catch (Exception e) {
                    log.error("Failed to generate tickets or send email for order {}: {}", order.getOrderNumber(), e.getMessage(), e);
                    // No lanzamos excepción para no revertir la transacción de la orden
                    // Los tickets se pueden generar manualmente después si es necesario
                }
                break;
            case "REJECTED":
            case "CANCELLED":
                order.markAsCancelled();
                log.info("Order {} marked as CANCELLED", order.getOrderNumber());
                break;
            case "IN_PROCESS":
            case "PENDING":
                // Mantener como PENDING_PAYMENT
                log.info("Order {} still PENDING_PAYMENT", order.getOrderNumber());
                break;
            default:
                log.warn("Unknown payment status: {} for order {}", request.getPaymentStatus(), order.getOrderNumber());
        }
        
        orderRepository.save(order);
    }
    
    @Override
    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    @Override
    public Order getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderNumber));
    }
    
    @Override
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
    }
    
    // ============================================
    // Helper Methods
    // ============================================
    
    private Order createOrderFromCartItems(ShoppingCart cart, Long adminId, List<CartItem> items, String sessionId, String timezone) {
        BigDecimal totalAmount = items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .userId(cart.getUserId())
                .cartId(cart.getId())
                .totalAmount(totalAmount)
                .adminId(adminId)
                .sessionId(sessionId)
                .clientTimezone(timezone)
                .status(Order.OrderStatus.PENDING_PAYMENT)
                .build();
        
        items.forEach(cartItem -> {
            OrderItem orderItem = OrderItem.fromCartItem(cartItem);
            order.addItem(orderItem);
        });
        
        return order;
    }

    /**
     * Genera tickets en event-service para cada entrada de la orden
     * Se ejecuta automáticamente cuando una orden es marcada como PAID
     * @return Lista de tickets generados con sus códigos QR
     */
    private List<TicketWithConsumptionsResponse> generateTicketsForOrder(Order order) {
        log.info("🎟️ Generating tickets for order: {}", order.getOrderNumber());
        
        List<TicketWithConsumptionsResponse> generatedTickets = new ArrayList<>();
        int ticketsGenerated = 0;
        int ticketsFailed = 0;
        
        // Por cada OrderItem (que representa entradas de un evento)
        for (OrderItem orderItem : order.getItems()) {
            Long eventId = orderItem.getEventId();
            Integer quantity = orderItem.getQuantity();
            
            log.info("Generating {} ticket(s) for event: {}", quantity, eventId);
            
            // Generar un ticket por cada entrada
            for (int i = 0; i < quantity; i++) {
                try {
                    // Preparar las consumiciones si existen
                    List<TicketConsumptionDTO> consumptions = new ArrayList<>();
                    if (orderItem.getConsumptions() != null && !orderItem.getConsumptions().isEmpty()) {
                        consumptions = orderItem.getConsumptions().stream()
                                .map(cons -> TicketConsumptionDTO.builder()
                                        .consumptionId(cons.getConsumptionId())
                                        .consumptionName(cons.getConsumptionName())
                                        .priceAtPurchase(cons.getUnitPrice())
                                        .quantity(cons.getQuantity())
                                        .build())
                                .collect(Collectors.toList());
                    }
                    
                    // Crear ticket con consumiciones
                    CreateTicketWithConsumptionsRequest ticketRequest = CreateTicketWithConsumptionsRequest.builder()
                            .userId(order.getUserId())
                            .eventId(eventId)
                            .consumptions(consumptions)
                            .purchasedAt(order.getPaidAt()) // Usar fecha de pago de la orden
                            .build();
                    
                    TicketWithConsumptionsResponse response = eventServiceClient.createTicketWithConsumptions(ticketRequest);
                    
                    // Check if ticket was created successfully (ticketId present)
                    if (response != null && response.getTicketId() != null) {
                        ticketsGenerated++;
                        generatedTickets.add(response);
                        log.info("✅ Ticket #{} generated: ID={}, QR={}", 
                                (i + 1), response.getTicketId(), response.getQrCode());
                    } else {
                        ticketsFailed++;
                        log.error("❌ Failed to generate ticket #{}: Response was null or missing ID", (i + 1));
                    }
                    
                } catch (Exception e) {
                    ticketsFailed++;
                    log.error("❌ Error generating ticket #{} for event {}: {}", 
                            (i + 1), eventId, e.getMessage(), e);
                }
            }
        }
        
        log.info("🎟️ Ticket generation completed for order {}: {} successful, {} failed",
                order.getOrderNumber(), ticketsGenerated, ticketsFailed);
        
        if (ticketsFailed > 0) {
            log.warn("⚠️ Some tickets failed to generate. Manual intervention may be required.");
        }
        
        return generatedTickets;
    }
    
    @Override
    public List<Order> getOrdersByOrganizerId(Long adminId) {
        log.info("Fetching all orders for organizer: {}", adminId);
        return orderRepository.findByAdminIdOrderByCreatedAtDesc(adminId);
    }
}


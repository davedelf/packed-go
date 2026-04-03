package com.packed_go.order_service.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad para el patrón Transactional Outbox.
 * Almacena eventos que deben ser publicados a RabbitMQ.
 * Se escribe en la misma transacción que la orden para garantizar atomicidad.
 */
@Entity
@Table(name = "outbox_events", indexes = {
    @jakarta.persistence.Index(name = "idx_outbox_status", columnList = "status"),
    @jakarta.persistence.Index(name = "idx_outbox_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId; // orderNumber

    @Column(name = "event_type", nullable = false, length = 100)
    @Enumerated(EnumType.STRING)
    private EventType eventType;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload; // JSON con los datos del evento

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EventStatus status = EventStatus.PENDING;

    @Column(name = "retry_count")
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (aggregateId == null) {
            aggregateId = UUID.randomUUID().toString();
        }
    }

    /**
     * Tipos de eventos para el outbox
     */
    public enum EventType {
        ORDER_PAID,           // Orden pagada - para generar tickets
        ORDER_CANCELLED,      // Orden cancelada - para liberar stock
        TICKETS_CREATED,      // Tickets generados - para confirmar email
        TICKETS_FAILED        // Tickets fallidos - para alertas
    }

    /**
     * Estado del evento
     */
    public enum EventStatus {
        PENDING,      // Esperando ser procesado
        PUBLISHED,    // Successfully publicado a RabbitMQ
        FAILED,       // Falló después de retries
        DLQ           // Enviado a Dead Letter Queue
    }

    /**
     * Marca el evento como publicado exitosamente
     */
    public void markAsPublished() {
        this.status = EventStatus.PUBLISHED;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * Marca el evento como fallido
     */
    public void markAsFailed(String errorMessage) {
        this.retryCount++;
        this.errorMessage = errorMessage;
        if (this.retryCount >= 5) {
            this.status = EventStatus.DLQ;
        }
    }
}

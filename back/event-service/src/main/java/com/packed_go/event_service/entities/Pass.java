package com.packed_go.event_service.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "passes", uniqueConstraints = {
    @UniqueConstraint(columnNames = "code")
})
@Getter
@Setter
public class Pass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean available = true;

    @Column(nullable = false)
    private boolean sold = false;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime soldAt;
    private Long soldToUserId;

    @Version
    private Long version;

    // Constructor por defecto
    public Pass() {}

    // Constructor para crear un pass con código
    public Pass(String code, Event event) {
        this.code = code;
        this.event = event;
        this.active = true;
        this.available = true;
        this.sold = false;
        this.createdAt = LocalDateTime.now();
    }
}

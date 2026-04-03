package com.packed_go.users_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "employees")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, unique = true)
    private Long document;

    @Column(nullable = false)
    private Long adminId; // ID del admin que creó este empleado

    @Column(nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ElementCollection
    @CollectionTable(
        name = "employee_events",
        joinColumns = @JoinColumn(name = "employee_id")
    )
    @Column(name = "event_id")
    private Set<Long> assignedEventIds = new HashSet<>();

    // Helper methods
    public void addEvent(Long eventId) {
        this.assignedEventIds.add(eventId);
    }

    public void removeEvent(Long eventId) {
        this.assignedEventIds.remove(eventId);
    }

    public boolean hasAccessToEvent(Long eventId) {
        return this.assignedEventIds.contains(eventId);
    }
}

package com.packed_go.event_service.repositories;
import com.packed_go.event_service.entities.EventCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface EventCategoryRepository extends JpaRepository<EventCategory, Long> {
    Optional<EventCategory> findById(Long id);

    List<EventCategory> findByActiveIsTrue();

    Optional<EventCategory> findByName(String name);

    // 🔒 Multitenant methods - filter by createdBy
    Optional<EventCategory> findByNameAndCreatedBy(String name, Long createdBy);
    List<EventCategory> findByCreatedBy(Long createdBy);
    List<EventCategory> findByActiveIsTrueAndCreatedBy(Long createdBy);
}

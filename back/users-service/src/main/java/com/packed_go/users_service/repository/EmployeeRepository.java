package com.packed_go.users_service.repository;

import com.packed_go.users_service.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmail(String email);

    Optional<Employee> findByUsername(String username);

    Optional<Employee> findByDocument(Long document);

    List<Employee> findByAdminId(Long adminId);

    List<Employee> findByAdminIdAndIsActive(Long adminId, Boolean isActive);

    @Query("SELECT e FROM Employee e JOIN e.assignedEventIds eventId WHERE eventId = :eventId AND e.isActive = true")
    List<Employee> findActiveEmployeesByEventId(@Param("eventId") Long eventId);

    boolean existsByEmail(String email);

    boolean existsByDocument(Long document);
}

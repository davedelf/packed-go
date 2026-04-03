package com.packedgo.payment_service.repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.packedgo.payment_service.model.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>{

    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findByStripeSessionId(String stripeSessionId);
    
    // ========== QUERIES PARA ESTADÍSTICAS ==========
    
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.adminId = :adminId")
    Long countByAdminId(@Param("adminId") Long adminId);
    
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.adminId = :adminId AND p.status = :status")
    Long countByAdminIdAndStatus(@Param("adminId") Long adminId, @Param("status") Payment.PaymentStatus status);
    
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.adminId = :adminId")
    BigDecimal sumAmountByAdminId(@Param("adminId") Long adminId);
    
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.adminId = :adminId AND p.status = :status")
    BigDecimal sumAmountByAdminIdAndStatus(@Param("adminId") Long adminId, @Param("status") Payment.PaymentStatus status);
    
    List<Payment> findByAdminId(Long adminId);
}

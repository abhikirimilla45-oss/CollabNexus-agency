package com.collabnexus.payments;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentRecord, Long> {

    Page<PaymentRecord> findByBrandId(Long brandId, Pageable pageable);

    Page<PaymentRecord> findByCreatorId(Long creatorId, Pageable pageable);

    Page<PaymentRecord> findByBrandIdAndStatus(Long brandId, PaymentStatus status, Pageable pageable);

    Page<PaymentRecord> findByCreatorIdAndStatus(Long creatorId, PaymentStatus status, Pageable pageable);

    Page<PaymentRecord> findByStatus(PaymentStatus status, Pageable pageable);

    Page<PaymentRecord> findByCollaborationId(Long collaborationId, Pageable pageable);

    Optional<PaymentRecord> findByIdAndCollaborationBrandUserId(Long id, Long userId);

    Optional<PaymentRecord> findByIdAndCollaborationCreatorUserId(Long id, Long userId);

    List<PaymentRecord> findByCreatorId(Long creatorId);

    List<PaymentRecord> findByBrandId(Long brandId);

    long countByCollaborationId(Long collaborationId);

    long countByBrandId(Long brandId);

    long countByCreatorId(Long creatorId);

    @Query("select coalesce(sum(payment.amount), 0) from PaymentRecord payment "
            + "where payment.collaboration.id = :collaborationId")
    BigDecimal sumAmountByCollaborationId(@Param("collaborationId") Long collaborationId);

    @Query("select coalesce(sum(payment.paidAmount), 0) from PaymentRecord payment "
            + "where payment.brand.id = :brandId")
    BigDecimal sumPaidByBrandId(@Param("brandId") Long brandId);

    @Query("select coalesce(sum(payment.amount), 0) from PaymentRecord payment "
            + "where payment.brand.id = :brandId")
    BigDecimal sumScheduledByBrandId(@Param("brandId") Long brandId);

    @Query("select coalesce(sum(payment.paidAmount), 0) from PaymentRecord payment "
            + "where payment.creator.id = :creatorId")
    BigDecimal sumPaidByCreatorId(@Param("creatorId") Long creatorId);

    @Query("select coalesce(sum(payment.amount), 0) from PaymentRecord payment "
            + "where payment.creator.id = :creatorId")
    BigDecimal sumScheduledByCreatorId(@Param("creatorId") Long creatorId);

    long countByBrandIdAndStatus(Long brandId, PaymentStatus status);

    long countByCreatorIdAndStatus(Long creatorId, PaymentStatus status);

    long countByStatus(PaymentStatus status);

    @Query("select coalesce(sum(payment.amount), 0) from PaymentRecord payment")
    BigDecimal sumAllScheduled();

    @Query("select coalesce(sum(payment.paidAmount), 0) from PaymentRecord payment")
    BigDecimal sumAllPaid();
}

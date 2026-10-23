package com.collabnexus.campaigns;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface CampaignRepository extends JpaRepository<Campaign, Long>, JpaSpecificationExecutor<Campaign> {

    Page<Campaign> findByBrandId(Long brandId, Pageable pageable);

    Optional<Campaign> findByIdAndBrandId(Long id, Long brandId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select campaign from Campaign campaign where campaign.id = :id")
    Optional<Campaign> findByIdForUpdate(@Param("id") Long id);

    long countByBrandId(Long brandId);

    long countByBrandIdAndStatus(Long brandId, CampaignStatus status);

    long countByStatus(CampaignStatus status);

    @Query("select coalesce(sum(campaign.maxBudget), 0) from Campaign campaign "
            + "where campaign.brand.id = :brandId")
    BigDecimal sumMaxBudgetByBrandId(@Param("brandId") Long brandId);
}

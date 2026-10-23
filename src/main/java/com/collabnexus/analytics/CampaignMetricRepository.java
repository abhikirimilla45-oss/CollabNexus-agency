package com.collabnexus.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CampaignMetricRepository extends JpaRepository<CampaignMetric, Long> {

    List<CampaignMetric> findByCampaignIdOrderByRecordedOnDescIdDesc(Long campaignId);

    Optional<CampaignMetric> findByIdAndCampaignBrandUserId(Long id, Long userId);

    @Query("select coalesce(sum(metric.reach), 0) from CampaignMetric metric "
            + "where metric.campaign.brand.id = :brandId")
    Long sumReachByBrandId(@Param("brandId") Long brandId);

    @Query("select coalesce(sum(metric.impressions), 0) from CampaignMetric metric "
            + "where metric.campaign.brand.id = :brandId")
    Long sumImpressionsByBrandId(@Param("brandId") Long brandId);

    @Query("select coalesce(sum(metric.clicks), 0) from CampaignMetric metric "
            + "where metric.campaign.brand.id = :brandId")
    Long sumClicksByBrandId(@Param("brandId") Long brandId);

    @Query("select coalesce(sum(metric.conversions), 0) from CampaignMetric metric "
            + "where metric.campaign.brand.id = :brandId")
    Long sumConversionsByBrandId(@Param("brandId") Long brandId);

    @Query("select coalesce(sum(metric.spend), 0) from CampaignMetric metric "
            + "where metric.campaign.brand.id = :brandId")
    BigDecimal sumSpendByBrandId(@Param("brandId") Long brandId);

    @Query("select coalesce(sum(metric.revenue), 0) from CampaignMetric metric "
            + "where metric.campaign.brand.id = :brandId")
    BigDecimal sumRevenueByBrandId(@Param("brandId") Long brandId);
}

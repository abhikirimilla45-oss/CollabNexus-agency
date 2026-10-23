package com.collabnexus.applications;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface CampaignApplicationRepository extends JpaRepository<CampaignApplication, Long>,
        JpaSpecificationExecutor<CampaignApplication> {

    boolean existsByCampaignIdAndCreatorId(Long campaignId, Long creatorId);

    Page<CampaignApplication> findByCampaignId(Long campaignId, Pageable pageable);

    Page<CampaignApplication> findByCampaignBrandId(Long brandId, Pageable pageable);

    Page<CampaignApplication> findByCreatorId(Long creatorId, Pageable pageable);

    Optional<CampaignApplication> findByIdAndCampaignBrandUserId(Long id, Long userId);

    Optional<CampaignApplication> findByIdAndCreatorUserId(Long id, Long userId);

    long countByCampaignIdAndStatus(Long campaignId, ApplicationStatus status);

    long countByCampaignId(Long campaignId);

    long countByCampaignBrandId(Long brandId);

    long countByCampaignBrandIdAndStatus(Long brandId, ApplicationStatus status);

    long countByCreatorId(Long creatorId);

    long countByCreatorIdAndStatus(Long creatorId, ApplicationStatus status);

    List<CampaignApplication> findByCampaignBrandId(Long brandId);

    List<CampaignApplication> findByCreatorId(Long creatorId);
}

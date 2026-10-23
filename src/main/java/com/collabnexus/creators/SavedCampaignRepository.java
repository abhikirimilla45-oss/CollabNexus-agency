package com.collabnexus.creators;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SavedCampaignRepository extends JpaRepository<SavedCampaign, Long> {

    Page<SavedCampaign> findByCreatorId(Long creatorId, Pageable pageable);

    Optional<SavedCampaign> findByCreatorIdAndCampaignId(Long creatorId, Long campaignId);

    boolean existsByCreatorIdAndCampaignId(Long creatorId, Long campaignId);

    @Query("select saved.campaign.id from SavedCampaign saved where saved.creator.id = :creatorId")
    List<Long> findCampaignIdsByCreatorId(@Param("creatorId") Long creatorId);
}

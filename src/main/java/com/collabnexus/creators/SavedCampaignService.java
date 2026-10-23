package com.collabnexus.creators;

import com.collabnexus.applications.CampaignApplicationRepository;
import com.collabnexus.campaigns.Campaign;
import com.collabnexus.campaigns.CampaignRepository;
import com.collabnexus.campaigns.CampaignStatus;
import com.collabnexus.campaigns.CampaignView;
import com.collabnexus.common.Exceptions;
import com.collabnexus.common.PageView;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SavedCampaignService {

    private final CreatorProfileRepository creatorProfileRepository;
    private final CampaignRepository campaignRepository;
    private final SavedCampaignRepository savedCampaignRepository;
    private final CampaignApplicationRepository applicationRepository;

    public SavedCampaignService(CreatorProfileRepository creatorProfileRepository,
                                CampaignRepository campaignRepository,
                                SavedCampaignRepository savedCampaignRepository,
                                CampaignApplicationRepository applicationRepository) {
        this.creatorProfileRepository = creatorProfileRepository;
        this.campaignRepository = campaignRepository;
        this.savedCampaignRepository = savedCampaignRepository;
        this.applicationRepository = applicationRepository;
    }

    @Transactional(readOnly = true)
    public PageView<CampaignView> list(Long userId, int page, int size) {
        CreatorProfile creator = profile(userId);
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 48);
        Page<SavedCampaign> results = savedCampaignRepository.findByCreatorId(creator.getId(),
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageView.of(results, saved -> CampaignView.from(saved.getCampaign(),
                applicationRepository.countByCampaignId(saved.getCampaign().getId())));
    }

    @Transactional(readOnly = true)
    public List<Long> ids(Long userId) {
        return savedCampaignRepository.findCampaignIdsByCreatorId(profile(userId).getId());
    }

    @Transactional
    public CampaignView save(Long userId, Long campaignId) {
        CreatorProfile creator = profile(userId);
        Campaign campaign = campaignRepository.findById(campaignId)
                .filter(found -> found.getStatus() == CampaignStatus.PUBLISHED)
                .orElseThrow(() -> Exceptions.notFound("Campaign not found."));

        if (!savedCampaignRepository.existsByCreatorIdAndCampaignId(creator.getId(), campaignId)) {
            try {
                savedCampaignRepository.saveAndFlush(new SavedCampaign(creator, campaign));
            } catch (DataIntegrityViolationException ignored) {
                // A concurrent request already saved the same campaign.
            }
        }
        return CampaignView.from(campaign, applicationRepository.countByCampaignId(campaignId));
    }

    @Transactional
    public void remove(Long userId, Long campaignId) {
        CreatorProfile creator = profile(userId);
        savedCampaignRepository.findByCreatorIdAndCampaignId(creator.getId(), campaignId)
                .ifPresent(savedCampaignRepository::delete);
    }

    private CreatorProfile profile(Long userId) {
        return creatorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> Exceptions.notFound("Creator profile not found."));
    }
}

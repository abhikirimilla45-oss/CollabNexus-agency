package com.collabnexus.creators;

import com.collabnexus.common.Exceptions;
import com.collabnexus.common.Urls;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CreatorAssetsService {

    private final CreatorProfileRepository creatorProfileRepository;
    private final CreatorSocialChannelRepository socialChannelRepository;
    private final CreatorPortfolioItemRepository portfolioItemRepository;

    public CreatorAssetsService(CreatorProfileRepository creatorProfileRepository,
                                CreatorSocialChannelRepository socialChannelRepository,
                                CreatorPortfolioItemRepository portfolioItemRepository) {
        this.creatorProfileRepository = creatorProfileRepository;
        this.socialChannelRepository = socialChannelRepository;
        this.portfolioItemRepository = portfolioItemRepository;
    }

    @Transactional(readOnly = true)
    public List<SocialChannelView> listSocialChannels(Long userId) {
        return socialChannelRepository.findByCreatorIdOrderByCreatedAtAsc(profile(userId).getId())
                .stream().map(SocialChannelView::from).toList();
    }

    @Transactional
    public SocialChannelView addSocialChannel(Long userId, SocialChannelRequest request) {
        CreatorProfile creator = profile(userId);
        CreatorSocialChannel channel = new CreatorSocialChannel(creator,
                request.platform().trim(), request.handle().trim());
        apply(channel, request);
        return SocialChannelView.from(socialChannelRepository.saveAndFlush(channel));
    }

    @Transactional
    public SocialChannelView updateSocialChannel(Long userId, Long channelId, SocialChannelRequest request) {
        CreatorSocialChannel channel = socialChannelRepository
                .findByIdAndCreatorUserId(channelId, userId)
                .orElseThrow(() -> Exceptions.notFound("Social channel not found."));
        apply(channel, request);
        return SocialChannelView.from(socialChannelRepository.saveAndFlush(channel));
    }

    @Transactional
    public void removeSocialChannel(Long userId, Long channelId) {
        CreatorSocialChannel channel = socialChannelRepository
                .findByIdAndCreatorUserId(channelId, userId)
                .orElseThrow(() -> Exceptions.notFound("Social channel not found."));
        socialChannelRepository.delete(channel);
    }

    @Transactional(readOnly = true)
    public List<PortfolioItemView> listPortfolioItems(Long userId) {
        return portfolioItemRepository.findByCreatorIdOrderByCreatedAtAsc(profile(userId).getId())
                .stream().map(PortfolioItemView::from).toList();
    }

    @Transactional
    public PortfolioItemView addPortfolioItem(Long userId, PortfolioItemRequest request) {
        CreatorProfile creator = profile(userId);
        CreatorPortfolioItem item = new CreatorPortfolioItem(creator,
                request.title().trim(), request.url().trim());
        apply(item, request);
        return PortfolioItemView.from(portfolioItemRepository.saveAndFlush(item));
    }

    @Transactional
    public PortfolioItemView updatePortfolioItem(Long userId, Long itemId, PortfolioItemRequest request) {
        CreatorPortfolioItem item = portfolioItemRepository
                .findByIdAndCreatorUserId(itemId, userId)
                .orElseThrow(() -> Exceptions.notFound("Portfolio item not found."));
        apply(item, request);
        return PortfolioItemView.from(portfolioItemRepository.saveAndFlush(item));
    }

    @Transactional
    public void removePortfolioItem(Long userId, Long itemId) {
        CreatorPortfolioItem item = portfolioItemRepository
                .findByIdAndCreatorUserId(itemId, userId)
                .orElseThrow(() -> Exceptions.notFound("Portfolio item not found."));
        portfolioItemRepository.delete(item);
    }

    private void apply(CreatorSocialChannel channel, SocialChannelRequest request) {
        channel.setPlatform(request.platform().trim());
        channel.setHandle(request.handle().trim());
        channel.setUrl(Urls.normalizeHttpUrl(request.url(), "Channel URL"));
        channel.setFollowersCount(request.followersCount());
        channel.setEngagementRate(request.engagementRate());
    }

    private void apply(CreatorPortfolioItem item, PortfolioItemRequest request) {
        item.setTitle(request.title().trim());
        item.setUrl(Urls.normalizeHttpUrl(request.url(), "Portfolio item URL"));
        item.setDescription(trimToNull(request.description()));
    }

    private CreatorProfile profile(Long userId) {
        return creatorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> Exceptions.notFound("Creator profile not found."));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

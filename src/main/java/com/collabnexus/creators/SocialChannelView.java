package com.collabnexus.creators;

import java.math.BigDecimal;

public record SocialChannelView(
        Long id,
        String platform,
        String handle,
        String url,
        Long followersCount,
        BigDecimal engagementRate,
        boolean verified
) {

    public static SocialChannelView from(CreatorSocialChannel channel) {
        return new SocialChannelView(
                channel.getId(),
                channel.getPlatform(),
                channel.getHandle(),
                channel.getUrl(),
                channel.getFollowersCount(),
                channel.getEngagementRate(),
                channel.isVerified()
        );
    }
}

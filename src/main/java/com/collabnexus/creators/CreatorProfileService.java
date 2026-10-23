package com.collabnexus.creators;

import com.collabnexus.common.Exceptions;
import com.collabnexus.common.Urls;
import com.collabnexus.users.User;
import com.collabnexus.users.UserRepository;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;

@Service
public class CreatorProfileService {

    private final CreatorProfileRepository creatorProfileRepository;
    private final UserRepository userRepository;

    public CreatorProfileService(CreatorProfileRepository creatorProfileRepository, UserRepository userRepository) {
        this.creatorProfileRepository = creatorProfileRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public CreatorView getForUser(Long userId) {
        return creatorProfileRepository.findByUserId(userId)
                .map(CreatorView::from)
                .orElseThrow(() -> Exceptions.notFound("Creator profile not found."));
    }

    @Transactional
    public CreatorView update(Long userId, UpdateCreatorRequest request) {
        CreatorProfile profile = creatorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> Exceptions.notFound("Creator profile not found."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> Exceptions.notFound("Account not found."));

        profile.setDisplayName(request.displayName().trim());
        profile.setHeadline(trimToNull(request.headline()));
        profile.setBio(trimToNull(request.bio()));
        profile.setNiche(request.niche().trim());
        profile.setPrimaryPlatform(request.primaryPlatform().trim());
        profile.setCountry(trimToNull(request.country()));
        profile.setLanguages(trimToNull(request.languages()));
        profile.setContentFormats(trimToNull(request.contentFormats()));
        profile.setBaseRate(request.baseRate());
        profile.setCurrency(normalizeCurrency(request.currency()));
        profile.setAvailability(request.availability() == null
                ? AvailabilityStatus.AVAILABLE : request.availability());
        profile.setPortfolioUrl(Urls.normalizeHttpUrl(request.portfolioUrl(), "Portfolio URL"));
        profile.setAvatarUrl(Urls.normalizeHttpUrl(request.avatarUrl(), "Avatar URL"));
        profile.setFollowersCount(request.followersCount());
        profile.setEngagementRate(request.engagementRate());
        profile.setAudienceCountries(trimToNull(request.audienceCountries()));
        user.setFullName(request.displayName().trim());
        userRepository.save(user);

        return CreatorView.from(creatorProfileRepository.saveAndFlush(profile));
    }

    private String normalizeCurrency(String value) {
        if (value == null || value.isBlank()) {
            return "USD";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record UpdateCreatorRequest(
            @NotBlank(message = "is required")
            @Size(max = 120, message = "must be at most 120 characters")
            String displayName,
            @Size(max = 160, message = "must be at most 160 characters")
            String headline,
            @Size(max = 2000, message = "must be at most 2000 characters")
            String bio,
            @NotBlank(message = "is required")
            @Size(max = 120, message = "must be at most 120 characters")
            String niche,
            @NotBlank(message = "is required")
            @Size(max = 60, message = "must be at most 60 characters")
            String primaryPlatform,
            @Size(max = 120, message = "must be at most 120 characters")
            String country,
            @Size(max = 255, message = "must be at most 255 characters")
            String languages,
            @Size(max = 255, message = "must be at most 255 characters")
            String contentFormats,
            @DecimalMin(value = "0.00", message = "must be zero or greater")
            @DecimalMax(value = "10000000.00", message = "must be at most 10,000,000")
            @Digits(integer = 8, fraction = 2, message = "supports up to 2 decimal places")
            BigDecimal baseRate,
            @Pattern(regexp = "^[A-Za-z]{3}$", message = "must be a three letter currency code")
            String currency,
            AvailabilityStatus availability,
            @Size(max = 500, message = "must be at most 500 characters")
            String portfolioUrl,
            @Size(max = 500, message = "must be at most 500 characters")
            String avatarUrl,
            @PositiveOrZero(message = "must be zero or greater")
            Long followersCount,
            @DecimalMin(value = "0.00", message = "must be zero or greater")
            @DecimalMax(value = "100.00", message = "must be at most 100")
            @Digits(integer = 3, fraction = 2, message = "supports up to 2 decimal places")
            BigDecimal engagementRate,
            @Size(max = 255, message = "must be at most 255 characters")
            String audienceCountries
    ) {
    }
}

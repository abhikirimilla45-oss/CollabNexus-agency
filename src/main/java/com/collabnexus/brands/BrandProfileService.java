package com.collabnexus.brands;

import com.collabnexus.common.Exceptions;
import com.collabnexus.common.Urls;
import com.collabnexus.users.User;
import com.collabnexus.users.UserRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BrandProfileService {

    private final BrandProfileRepository brandProfileRepository;
    private final UserRepository userRepository;

    public BrandProfileService(BrandProfileRepository brandProfileRepository, UserRepository userRepository) {
        this.brandProfileRepository = brandProfileRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public BrandView getForUser(Long userId) {
        return brandProfileRepository.findByUserId(userId)
                .map(BrandView::from)
                .orElseThrow(() -> Exceptions.notFound("Brand profile not found."));
    }

    @Transactional
    public BrandView update(Long userId, UpdateBrandRequest request) {
        BrandProfile profile = brandProfileRepository.findByUserId(userId)
                .orElseThrow(() -> Exceptions.notFound("Brand profile not found."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> Exceptions.notFound("Account not found."));

        profile.setCompanyName(request.companyName().trim());
        profile.setWebsite(Urls.normalizeHttpUrl(request.companyWebsite(), "Company website"));
        profile.setIndustry(trimToNull(request.industry()));
        profile.setCountry(trimToNull(request.country()));
        profile.setDescription(trimToNull(request.description()));
        profile.setLogoUrl(Urls.normalizeHttpUrl(request.logoUrl(), "Logo URL"));
        user.setFullName(request.contactName().trim());
        userRepository.save(user);

        return BrandView.from(brandProfileRepository.saveAndFlush(profile));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record UpdateBrandRequest(
            @NotBlank(message = "is required")
            @Size(max = 160, message = "must be at most 160 characters")
            String companyName,
            @Size(max = 255, message = "must be at most 255 characters")
            String companyWebsite,
            @Size(max = 120, message = "must be at most 120 characters")
            String industry,
            @Size(max = 120, message = "must be at most 120 characters")
            String country,
            @Size(max = 2000, message = "must be at most 2000 characters")
            String description,
            @Size(max = 500, message = "must be at most 500 characters")
            String logoUrl,
            @NotBlank(message = "is required")
            @Size(max = 120, message = "must be at most 120 characters")
            String contactName
    ) {
    }
}

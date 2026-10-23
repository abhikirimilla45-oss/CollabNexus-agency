package com.collabnexus.auth;

import com.collabnexus.brands.BrandProfile;
import com.collabnexus.brands.BrandProfileRepository;
import com.collabnexus.brands.BrandView;
import com.collabnexus.common.Exceptions;
import com.collabnexus.creators.AvailabilityStatus;
import com.collabnexus.creators.CreatorProfile;
import com.collabnexus.creators.CreatorProfileRepository;
import com.collabnexus.creators.CreatorView;
import com.collabnexus.users.Role;
import com.collabnexus.users.User;
import com.collabnexus.users.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BrandProfileRepository brandProfileRepository;
    private final CreatorProfileRepository creatorProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       BrandProfileRepository brandProfileRepository,
                       CreatorProfileRepository creatorProfileRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.brandProfileRepository = brandProfileRepository;
        this.creatorProfileRepository = creatorProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public SessionView register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw Exceptions.conflict("EMAIL_IN_USE", "An account with this email address already exists.");
        }

        Role role = parseRole(request.role());
        String fullName = request.fullName().trim();
        User user = new User(email, passwordEncoder.encode(request.password()), fullName, role);
        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw Exceptions.conflict("EMAIL_IN_USE", "An account with this email address already exists.");
        }

        BrandView brandView = null;
        CreatorView creatorView = null;

        if (role == Role.BRAND) {
            String companyName = require(request.companyName(), "Company name is required for a brand account.");
            String website = normalizeUrl(request.companyWebsite(), "Company website");
            BrandProfile profile = new BrandProfile(user, companyName);
            profile.setWebsite(website);
            profile.setCountry(trimToNull(request.country()));
            brandView = BrandView.from(brandProfileRepository.save(profile));
        } else {
            String niche = require(request.primaryNiche(), "Primary niche is required for a creator account.");
            String platform = require(request.primaryPlatform(),
                    "Primary platform is required for a creator account.");
            CreatorProfile profile = new CreatorProfile(user, fullName);
            profile.setNiche(niche);
            profile.setPrimaryPlatform(platform);
            profile.setCountry(trimToNull(request.country()));
            profile.setAvailability(AvailabilityStatus.AVAILABLE);
            creatorView = CreatorView.from(creatorProfileRepository.save(profile));
        }

        return SessionView.of(user, brandView, creatorView);
    }

    @Transactional(readOnly = true)
    public SessionView sessionFor(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> Exceptions.unauthorized("Your account could not be found. Please sign in again."));
        BrandView brand = brandProfileRepository.findByUserId(userId).map(BrandView::from).orElse(null);
        CreatorView creator = creatorProfileRepository.findByUserId(userId).map(CreatorView::from).orElse(null);
        return SessionView.of(user, brand, creator);
    }

    private Role parseRole(String value) {
        try {
            Role role = Role.valueOf(value.trim().toUpperCase(Locale.ROOT));
            if (role == Role.ADMIN) {
                throw Exceptions.badRequest("INVALID_ROLE", "Administrator accounts cannot be self-registered.");
            }
            return role;
        } catch (IllegalArgumentException ex) {
            throw Exceptions.badRequest("INVALID_ROLE", "Choose either a brand or creator account.");
        }
    }

    private String require(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw Exceptions.badRequest("VALIDATION_FAILED", message);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeUrl(String value, String label) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String candidate = value.trim();
        try {
            URI uri = new URI(candidate);
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                    || uri.getHost() == null) {
                throw Exceptions.badRequest("INVALID_URL", label + " must be a full URL starting with http:// or https://.");
            }
            return candidate;
        } catch (URISyntaxException ex) {
            throw Exceptions.badRequest("INVALID_URL", label + " must be a full URL starting with http:// or https://.");
        }
    }
}

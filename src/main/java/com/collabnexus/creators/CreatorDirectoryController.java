package com.collabnexus.creators;

import com.collabnexus.common.ApiResponse;
import com.collabnexus.common.Exceptions;
import com.collabnexus.common.PageView;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/api/creators")
public class CreatorDirectoryController {

    private static final Set<String> SORTABLE = Set.of("displayName", "baseRate", "followersCount",
            "engagementRate", "updatedAt", "createdAt");

    private final CreatorProfileRepository creatorProfileRepository;

    public CreatorDirectoryController(CreatorProfileRepository creatorProfileRepository) {
        this.creatorProfileRepository = creatorProfileRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ApiResponse<PageView<CreatorView>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String niche,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) AvailabilityStatus availability,
            @RequestParam(required = false) Long minFollowers,
            @RequestParam(required = false) Long maxFollowers,
            @RequestParam(required = false) BigDecimal minRate,
            @RequestParam(required = false) BigDecimal maxRate,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String audienceCountry,
            @RequestParam(required = false) BigDecimal minEngagement,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "displayName") String sort,
            @RequestParam(defaultValue = "asc") String direction) {

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 48);
        String sortField = SORTABLE.contains(sort) ? sort : "displayName";
        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction)
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        Specification<CreatorProfile> spec = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(q)) {
                String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("displayName")), like),
                        builder.like(builder.lower(root.get("headline")), like),
                        builder.like(builder.lower(root.get("niche")), like),
                        builder.like(builder.lower(root.get("bio")), like)
                ));
            }
            if (StringUtils.hasText(niche)) {
                predicates.add(builder.equal(builder.lower(root.get("niche")),
                        niche.trim().toLowerCase(Locale.ROOT)));
            }
            if (StringUtils.hasText(platform)) {
                predicates.add(builder.equal(builder.lower(root.get("primaryPlatform")),
                        platform.trim().toLowerCase(Locale.ROOT)));
            }
            if (StringUtils.hasText(country)) {
                predicates.add(builder.equal(builder.lower(root.get("country")),
                        country.trim().toLowerCase(Locale.ROOT)));
            }
            if (availability != null) {
                predicates.add(builder.equal(root.get("availability"), availability));
            }
            if (minFollowers != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("followersCount"), minFollowers));
            }
            if (maxFollowers != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("followersCount"), maxFollowers));
            }
            if (minRate != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("baseRate"), minRate));
            }
            if (maxRate != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("baseRate"), maxRate));
            }
            if (StringUtils.hasText(language)) {
                predicates.add(builder.like(builder.lower(root.get("languages")),
                        "%" + language.trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (StringUtils.hasText(audienceCountry)) {
                predicates.add(builder.like(builder.lower(root.get("audienceCountries")),
                        "%" + audienceCountry.trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (minEngagement != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("engagementRate"), minEngagement));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };

        Page<CreatorProfile> results = creatorProfileRepository.findAll(spec,
                PageRequest.of(safePage, safeSize, Sort.by(sortDirection, sortField)));
        return ApiResponse.ok(PageView.of(results, CreatorView::from));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ApiResponse<CreatorView> get(@PathVariable Long id) {
        CreatorProfile profile = creatorProfileRepository.findById(id)
                .orElseThrow(() -> Exceptions.notFound("Creator profile not found."));
        return ApiResponse.ok(CreatorView.from(profile));
    }
}

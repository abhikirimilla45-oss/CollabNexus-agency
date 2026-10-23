package com.collabnexus.admin;

import com.collabnexus.common.Exceptions;
import com.collabnexus.moderation.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class CategoryService {

    private final CampaignCategoryRepository repository;
    private final AuditLogService auditLogService;

    public CategoryService(CampaignCategoryRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<CategoryView> listAll() {
        return repository.findAllByOrderBySortOrderAscNameAsc().stream().map(CategoryView::from).toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryView> listActive() {
        return repository.findByActiveTrueOrderBySortOrderAscNameAsc().stream()
                .map(CategoryView::from).toList();
    }

    @Transactional
    public CategoryView create(CategoryRequest request, Long actorId) {
        String name = request.name().trim();
        String slug = slugFor(request, name);
        if (repository.existsByNameIgnoreCase(name)) {
            throw Exceptions.conflict("CATEGORY_EXISTS", "A category with this name already exists.");
        }
        if (repository.existsBySlugIgnoreCase(slug)) {
            throw Exceptions.conflict("CATEGORY_SLUG_EXISTS", "A category with this slug already exists.");
        }
        CampaignCategory category = new CampaignCategory(name, slug);
        apply(category, request);
        category = repository.saveAndFlush(category);
        auditLogService.record(actorId, "CATEGORY_CREATED", "CampaignCategory", category.getId(),
                "Category \"" + category.getName() + "\" created.");
        return CategoryView.from(category);
    }

    @Transactional
    public CategoryView update(Long id, CategoryRequest request, Long actorId) {
        CampaignCategory category = repository.findById(id)
                .orElseThrow(() -> Exceptions.notFound("Category not found."));
        String name = request.name().trim();
        String slug = slugFor(request, name);
        if (repository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw Exceptions.conflict("CATEGORY_EXISTS", "A category with this name already exists.");
        }
        if (repository.existsBySlugIgnoreCaseAndIdNot(slug, id)) {
            throw Exceptions.conflict("CATEGORY_SLUG_EXISTS", "A category with this slug already exists.");
        }
        category.setName(name);
        category.setSlug(slug);
        apply(category, request);
        category = repository.saveAndFlush(category);
        auditLogService.record(actorId, "CATEGORY_UPDATED", "CampaignCategory", category.getId(),
                "Category \"" + category.getName() + "\" updated.");
        return CategoryView.from(category);
    }

    private void apply(CampaignCategory category, CategoryRequest request) {
        category.setDescription(blankToNull(request.description()));
        category.setSortOrder(request.sortOrder() == null ? 100 : request.sortOrder());
        if (request.active() != null) {
            category.setActive(request.active());
        } else if (category.getId() == null) {
            category.setActive(true);
        }
    }

    private String slugFor(CategoryRequest request, String name) {
        String source = request.slug() == null || request.slug().isBlank() ? name : request.slug().trim();
        String slug = source.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (slug.isBlank()) {
            throw Exceptions.badRequest("INVALID_SLUG", "The category slug cannot be empty.");
        }
        return slug;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record CategoryView(Long id, String name, String slug, String description,
                               boolean active, int sortOrder, Instant createdAt, Instant updatedAt) {
        static CategoryView from(CampaignCategory category) {
            return new CategoryView(category.getId(), category.getName(), category.getSlug(),
                    category.getDescription(), category.isActive(), category.getSortOrder(),
                    category.getCreatedAt(), category.getUpdatedAt());
        }
    }
}

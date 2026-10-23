package com.collabnexus.campaigns;

import com.collabnexus.brands.BrandProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "campaigns")
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private BrandProfile brand;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(length = 2000)
    private String objectives;

    @Column(name = "product_details", length = 2000)
    private String productDetails;

    @Column(name = "target_audience", length = 2000)
    private String targetAudience;

    @Column(length = 255)
    private String platforms;

    @Column(length = 255)
    private String niches;

    @Column(name = "content_formats", length = 255)
    private String contentFormats;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "application_deadline", nullable = false)
    private LocalDate applicationDeadline;

    @Column(name = "min_budget", nullable = false, precision = 12, scale = 2)
    private BigDecimal minBudget;

    @Column(name = "max_budget", nullable = false, precision = 12, scale = 2)
    private BigDecimal maxBudget;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "compensation_model", nullable = false, length = 30)
    private CompensationModel compensationModel = CompensationModel.FIXED_FEE;

    @Column(name = "creators_required", nullable = false)
    private int creatorsRequired = 1;

    @Column(name = "deliverable_requirements", length = 2000)
    private String deliverableRequirements;

    @Column(name = "review_rules", length = 2000)
    private String reviewRules;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CampaignStatus status = CampaignStatus.DRAFT;

    @Column(length = 120)
    private String category;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected Campaign() {
    }

    public Campaign(BrandProfile brand, String title) {
        this.brand = brand;
        this.title = title;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public BrandProfile getBrand() {
        return brand;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getObjectives() {
        return objectives;
    }

    public void setObjectives(String objectives) {
        this.objectives = objectives;
    }

    public String getProductDetails() {
        return productDetails;
    }

    public void setProductDetails(String productDetails) {
        this.productDetails = productDetails;
    }

    public String getTargetAudience() {
        return targetAudience;
    }

    public void setTargetAudience(String targetAudience) {
        this.targetAudience = targetAudience;
    }

    public String getPlatforms() {
        return platforms;
    }

    public void setPlatforms(String platforms) {
        this.platforms = platforms;
    }

    public String getNiches() {
        return niches;
    }

    public void setNiches(String niches) {
        this.niches = niches;
    }

    public String getContentFormats() {
        return contentFormats;
    }

    public void setContentFormats(String contentFormats) {
        this.contentFormats = contentFormats;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDate getApplicationDeadline() {
        return applicationDeadline;
    }

    public void setApplicationDeadline(LocalDate applicationDeadline) {
        this.applicationDeadline = applicationDeadline;
    }

    public BigDecimal getMinBudget() {
        return minBudget;
    }

    public void setMinBudget(BigDecimal minBudget) {
        this.minBudget = minBudget;
    }

    public BigDecimal getMaxBudget() {
        return maxBudget;
    }

    public void setMaxBudget(BigDecimal maxBudget) {
        this.maxBudget = maxBudget;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public CompensationModel getCompensationModel() {
        return compensationModel;
    }

    public void setCompensationModel(CompensationModel compensationModel) {
        this.compensationModel = compensationModel;
    }

    public int getCreatorsRequired() {
        return creatorsRequired;
    }

    public void setCreatorsRequired(int creatorsRequired) {
        this.creatorsRequired = creatorsRequired;
    }

    public String getDeliverableRequirements() {
        return deliverableRequirements;
    }

    public void setDeliverableRequirements(String deliverableRequirements) {
        this.deliverableRequirements = deliverableRequirements;
    }

    public String getReviewRules() {
        return reviewRules;
    }

    public void setReviewRules(String reviewRules) {
        this.reviewRules = reviewRules;
    }

    public CampaignStatus getStatus() {
        return status;
    }

    public void setStatus(CampaignStatus status) {
        this.status = status;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}

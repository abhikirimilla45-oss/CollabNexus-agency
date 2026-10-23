package com.collabnexus.creators;

import com.collabnexus.campaigns.Campaign;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "saved_campaigns",
        uniqueConstraints = @UniqueConstraint(name = "uk_saved_campaign_creator",
                columnNames = {"creator_id", "campaign_id"}))
public class SavedCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private CreatorProfile creator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected SavedCampaign() {
    }

    public SavedCampaign(CreatorProfile creator, Campaign campaign) {
        this.creator = creator;
        this.campaign = campaign;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public CreatorProfile getCreator() {
        return creator;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

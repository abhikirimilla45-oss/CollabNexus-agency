package com.collabnexus.messaging;

import com.collabnexus.brands.BrandProfile;
import com.collabnexus.collaborations.Collaboration;
import com.collabnexus.creators.CreatorProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "conversations",
        uniqueConstraints = @UniqueConstraint(name = "uk_conversation_collaboration",
                columnNames = "collaboration_id"),
        indexes = {
                @Index(name = "idx_conversation_brand", columnList = "brand_id"),
                @Index(name = "idx_conversation_creator", columnList = "creator_id")
        })
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collaboration_id", nullable = false, unique = true)
    private Collaboration collaboration;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private BrandProfile brand;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private CreatorProfile creator;

    @Column(nullable = false, length = 160)
    private String subject;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Conversation() {
    }

    public Conversation(Collaboration collaboration, String subject) {
        this.collaboration = collaboration;
        this.brand = collaboration.getBrand();
        this.creator = collaboration.getCreator();
        this.subject = subject;
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

    public Collaboration getCollaboration() {
        return collaboration;
    }

    public BrandProfile getBrand() {
        return brand;
    }

    public CreatorProfile getCreator() {
        return creator;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Instant getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(Instant lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

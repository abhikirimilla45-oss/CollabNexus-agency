package com.collabnexus.deliverables;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliverableReviewRepository extends JpaRepository<DeliverableReview, Long> {

    List<DeliverableReview> findByDeliverableIdOrderByCreatedAtDesc(Long deliverableId);
}

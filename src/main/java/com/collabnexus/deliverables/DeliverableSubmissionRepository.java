package com.collabnexus.deliverables;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliverableSubmissionRepository extends JpaRepository<DeliverableSubmission, Long> {

    List<DeliverableSubmission> findByDeliverableIdOrderByVersionNumberDesc(Long deliverableId);

    Optional<DeliverableSubmission> findByDeliverableIdAndId(Long deliverableId, Long id);

    Optional<DeliverableSubmission> findFirstByDeliverableIdOrderByVersionNumberDesc(Long deliverableId);
}

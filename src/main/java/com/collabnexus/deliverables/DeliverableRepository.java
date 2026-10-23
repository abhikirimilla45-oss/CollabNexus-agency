package com.collabnexus.deliverables;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeliverableRepository extends JpaRepository<Deliverable, Long>,
        JpaSpecificationExecutor<Deliverable> {

    List<Deliverable> findByCollaborationIdOrderByDueDateAsc(Long collaborationId);

    Optional<Deliverable> findByIdAndCollaborationBrandUserId(Long id, Long userId);

    Optional<Deliverable> findByIdAndCollaborationCreatorUserId(Long id, Long userId);

    long countByCollaborationId(Long collaborationId);

    long countByCollaborationIdAndStatus(Long collaborationId, DeliverableStatus status);

    long countByCollaborationIdAndStatusIn(Long collaborationId,
                                           Collection<DeliverableStatus> statuses);

    long countByCollaborationIdAndDueDateBeforeAndStatusIn(Long collaborationId, LocalDate date,
                                                           Collection<DeliverableStatus> statuses);

    long countByCollaborationBrandUserIdAndStatus(Long userId, DeliverableStatus status);

    long countByCollaborationBrandUserId(Long userId);

    long countByCollaborationCreatorUserId(Long userId);

    long countByCollaborationCreatorUserIdAndStatus(Long userId, DeliverableStatus status);

    long countByCollaborationBrandUserIdAndDueDateBetweenAndStatusIn(Long userId, LocalDate start,
                                                                     LocalDate end,
                                                                     Collection<DeliverableStatus> statuses);

    long countByCollaborationCreatorUserIdAndDueDateBetweenAndStatusIn(Long userId, LocalDate start,
                                                                        LocalDate end,
                                                                        Collection<DeliverableStatus> statuses);

    long countByCollaborationCreatorUserIdAndDueDateBeforeAndStatusIn(Long userId, LocalDate date,
                                                                      Collection<DeliverableStatus> statuses);
}

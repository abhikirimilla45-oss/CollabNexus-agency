package com.collabnexus.collaborations;

import com.collabnexus.common.ApiResponse;
import com.collabnexus.common.PageView;
import com.collabnexus.contracts.ContractService;
import com.collabnexus.contracts.ContractView;
import com.collabnexus.deliverables.DeliverableService;
import com.collabnexus.deliverables.DeliverableView;
import com.collabnexus.security.SecurityUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/collaborations")
public class CollaborationController {

    private final CollaborationService collaborationService;
    private final DeliverableService deliverableService;
    private final ContractService contractService;

    public CollaborationController(CollaborationService collaborationService,
                                   DeliverableService deliverableService,
                                   ContractService contractService) {
        this.collaborationService = collaborationService;
        this.deliverableService = deliverableService;
        this.contractService = contractService;
    }

    @GetMapping
    public ApiResponse<PageView<CollaborationView>> list(@AuthenticationPrincipal SecurityUser user,
                                                         @RequestParam(required = false) CollaborationStatus status,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "8") int size) {
        return ApiResponse.ok(collaborationService.list(user.getId(), user.getRole(), status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<CollaborationView> detail(@AuthenticationPrincipal SecurityUser user,
                                                 @PathVariable Long id) {
        return ApiResponse.ok(collaborationService.detail(user.getId(), user.getRole(), id));
    }

    @GetMapping("/{id}/deliverables")
    public ApiResponse<List<DeliverableView>> deliverables(@AuthenticationPrincipal SecurityUser user,
                                                           @PathVariable Long id) {
        return ApiResponse.ok(deliverableService.listForCollaboration(user.getId(), user.getRole(), id));
    }

    @GetMapping("/{id}/contract")
    public ApiResponse<ContractView> contract(@AuthenticationPrincipal SecurityUser user,
                                              @PathVariable Long id) {
        return ApiResponse.ok(contractService.forCollaboration(user.getId(), user.getRole(), id));
    }

    @PostMapping("/{id}/start")
    public ApiResponse<CollaborationView> start(@AuthenticationPrincipal SecurityUser user,
                                                @PathVariable Long id) {
        return ApiResponse.ok(collaborationService.start(user.getId(), user.getRole(), id),
                "Collaboration moved into progress.");
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<CollaborationView> complete(@AuthenticationPrincipal SecurityUser user,
                                                   @PathVariable Long id) {
        return ApiResponse.ok(collaborationService.complete(user.getId(), user.getRole(), id),
                "Collaboration marked complete.");
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<CollaborationView> cancel(@AuthenticationPrincipal SecurityUser user,
                                                 @PathVariable Long id) {
        return ApiResponse.ok(collaborationService.cancel(user.getId(), user.getRole(), id),
                "Collaboration cancelled.");
    }
}

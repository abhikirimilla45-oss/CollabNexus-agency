package com.collabnexus.contracts;

import com.collabnexus.common.ApiResponse;
import com.collabnexus.security.SecurityUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/brand/contracts")
public class BrandContractController {

    private final ContractService contractService;

    public BrandContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @PutMapping("/{id}")
    public ApiResponse<ContractView> update(@AuthenticationPrincipal SecurityUser user,
                                            @PathVariable Long id,
                                            @Valid @RequestBody ContractTermsRequest request) {
        return ApiResponse.ok(contractService.updateTerms(user.getId(), id, request),
                "Contract terms saved as a new version.");
    }

    @PostMapping("/{id}/send")
    public ApiResponse<ContractView> send(@AuthenticationPrincipal SecurityUser user,
                                          @PathVariable Long id) {
        return ApiResponse.ok(contractService.send(user.getId(), id),
                "Contract sent for acknowledgement.");
    }
}

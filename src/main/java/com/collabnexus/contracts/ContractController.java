package com.collabnexus.contracts;

import com.collabnexus.common.ApiResponse;
import com.collabnexus.common.PageView;
import com.collabnexus.security.SecurityUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @GetMapping
    public ApiResponse<PageView<ContractView>> list(@AuthenticationPrincipal SecurityUser user,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "8") int size) {
        return ApiResponse.ok(contractService.list(user.getId(), user.getRole(), page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ContractView> detail(@AuthenticationPrincipal SecurityUser user,
                                            @PathVariable Long id) {
        return ApiResponse.ok(contractService.detail(user.getId(), user.getRole(), id));
    }

    @PostMapping("/{id}/acknowledge")
    public ApiResponse<ContractView> acknowledge(@AuthenticationPrincipal SecurityUser user,
                                                 @PathVariable Long id) {
        return ApiResponse.ok(contractService.acknowledge(user.getId(), user.getRole(), id),
                "Contract terms acknowledged.");
    }
}

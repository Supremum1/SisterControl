package com.sistercontrol.server.controller;

import com.sistercontrol.server.dto.ApiDtos.*;
import com.sistercontrol.server.security.CurrentUser;
import com.sistercontrol.server.service.PairingService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PairingController {
    private final PairingService pairingService;
    private final CurrentUser currentUser;

    public PairingController(PairingService pairingService, CurrentUser currentUser) {
        this.pairingService = pairingService;
        this.currentUser = currentUser;
    }

    @PostMapping("/child/create-code")
    @PreAuthorize("hasRole('CHILD')")
    public CodeResponse createCode() {
        return pairingService.createCode(currentUser.get());
    }

    @PostMapping("/parent/pair")
    @PreAuthorize("hasRole('PARENT')")
    public OkResponse pair(@Valid @RequestBody PairRequest request) {
        pairingService.pair(currentUser.get(), request.code());
        return OkResponse.success();
    }

    @GetMapping("/child/pair-status")
    @PreAuthorize("hasRole('CHILD')")
    public PairStatusResponse childPairStatus() {
        return pairingService.childPairStatus(currentUser.get());
    }

    @GetMapping("/parent/connections")
    @PreAuthorize("hasRole('PARENT')")
    public ParentConnectionResponse parentConnections() {
        return pairingService.parentConnection(currentUser.get());
    }

    @GetMapping("/child/connection-info")
    @PreAuthorize("hasRole('CHILD')")
    public ChildConnectionResponse childConnectionInfo() {
        return pairingService.childConnection(currentUser.get());
    }
}

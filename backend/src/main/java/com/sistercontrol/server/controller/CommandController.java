package com.sistercontrol.server.controller;

import com.sistercontrol.server.dto.ApiDtos.CommandsResponse;
import com.sistercontrol.server.dto.ApiDtos.OkResponse;
import com.sistercontrol.server.dto.ApiDtos.SafeZoneRequest;
import com.sistercontrol.server.security.CurrentUser;
import com.sistercontrol.server.service.CommandService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class CommandController {
    private final CommandService commandService;
    private final CurrentUser currentUser;

    public CommandController(CommandService commandService, CurrentUser currentUser) {
        this.commandService = commandService;
        this.currentUser = currentUser;
    }

    @PostMapping("/parent/request-gps")
    @PreAuthorize("hasRole('PARENT')")
    public OkResponse requestGps() {
        commandService.requestGps(currentUser.get());
        return OkResponse.success();
    }

    @PostMapping("/parent/safe-zone")
    @PreAuthorize("hasRole('PARENT')")
    public OkResponse setSafeZone(@Valid @RequestBody SafeZoneRequest request) {
        commandService.setSafeZone(currentUser.get(), request);
        return OkResponse.success();
    }

    @GetMapping("/child/commands")
    @PreAuthorize("hasRole('CHILD')")
    public CommandsResponse childCommands(@RequestParam(defaultValue = "10") int limit) {
        return commandService.childCommands(currentUser.get(), limit);
    }

    @PostMapping("/child/commands/{id}/done")
    @PreAuthorize("hasRole('CHILD')")
    public OkResponse commandDone(@PathVariable UUID id) {
        return new OkResponse(commandService.markDone(currentUser.get(), id));
    }
}

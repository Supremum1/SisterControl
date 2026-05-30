package com.sistercontrol.server.controller;

import com.sistercontrol.server.dto.ApiDtos.LocationRequest;
import com.sistercontrol.server.dto.ApiDtos.LocationResponse;
import com.sistercontrol.server.dto.ApiDtos.OkResponse;
import com.sistercontrol.server.security.CurrentUser;
import com.sistercontrol.server.service.LocationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class LocationController {
    private final LocationService locationService;
    private final CurrentUser currentUser;

    public LocationController(LocationService locationService, CurrentUser currentUser) {
        this.locationService = locationService;
        this.currentUser = currentUser;
    }

    @PostMapping("/child/location")
    @PreAuthorize("hasRole('CHILD')")
    public OkResponse childLocation(@Valid @RequestBody LocationRequest request) {
        locationService.upload(currentUser.get(), request);
        return OkResponse.success();
    }

    @GetMapping("/parent/child-location")
    @PreAuthorize("hasRole('PARENT')")
    public LocationResponse childLocation() {
        return locationService.currentChildLocation(currentUser.get());
    }
}

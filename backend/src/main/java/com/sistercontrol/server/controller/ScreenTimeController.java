package com.sistercontrol.server.controller;

import com.sistercontrol.server.dto.ApiDtos.*;
import com.sistercontrol.server.security.CurrentUser;
import com.sistercontrol.server.service.ScreenTimeService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api")
public class ScreenTimeController {
    private final ScreenTimeService screenTimeService;
    private final CurrentUser currentUser;

    public ScreenTimeController(ScreenTimeService screenTimeService, CurrentUser currentUser) {
        this.screenTimeService = screenTimeService;
        this.currentUser = currentUser;
    }

    @PostMapping("/parent/screen-time")
    @PreAuthorize("hasRole('PARENT')")
    public OkResponse savePolicy(@Valid @RequestBody ScreenTimePolicyRequest request) {
        screenTimeService.saveParentPolicy(currentUser.get(), request);
        return OkResponse.success();
    }

    @GetMapping("/parent/screen-time")
    @PreAuthorize("hasRole('PARENT')")
    public ScreenTimePolicyResponse parentPolicy() {
        return screenTimeService.parentPolicy(currentUser.get());
    }

    @GetMapping("/child/screen-time")
    @PreAuthorize("hasRole('CHILD')")
    public ScreenTimePolicyResponse childPolicy() {
        return screenTimeService.childPolicy(currentUser.get());
    }

    @PostMapping("/child/screen-time-usage")
    @PreAuthorize("hasRole('CHILD')")
    public OkResponse uploadUsage(@Valid @RequestBody UsageUploadRequest request) {
        screenTimeService.uploadUsage(currentUser.get(), request);
        return OkResponse.success();
    }

    @GetMapping("/parent/screen-time-usage")
    @PreAuthorize("hasRole('PARENT')")
    public ParentUsageResponse parentUsage(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate day
    ) {
        return screenTimeService.parentUsage(currentUser.get(), day);
    }
}

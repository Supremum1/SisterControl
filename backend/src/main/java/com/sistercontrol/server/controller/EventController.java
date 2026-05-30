package com.sistercontrol.server.controller;

import com.sistercontrol.server.dto.ApiDtos.EventRequest;
import com.sistercontrol.server.dto.ApiDtos.EventsResponse;
import com.sistercontrol.server.dto.ApiDtos.OkResponse;
import com.sistercontrol.server.security.CurrentUser;
import com.sistercontrol.server.service.EventService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class EventController {
    private final EventService eventService;
    private final CurrentUser currentUser;

    public EventController(EventService eventService, CurrentUser currentUser) {
        this.eventService = eventService;
        this.currentUser = currentUser;
    }

    @PostMapping("/child/event")
    @PreAuthorize("hasRole('CHILD')")
    public OkResponse childEvent(@Valid @RequestBody EventRequest request) {
        eventService.createChildEvent(currentUser.get(), request);
        return OkResponse.success();
    }

    @GetMapping("/parent/events")
    @PreAuthorize("hasRole('PARENT')")
    public EventsResponse parentEvents(@RequestParam(defaultValue = "50") int limit) {
        return eventService.parentEvents(currentUser.get(), limit);
    }
}

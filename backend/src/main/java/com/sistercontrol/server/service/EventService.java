package com.sistercontrol.server.service;

import com.sistercontrol.server.dto.ApiDtos.EventRequest;
import com.sistercontrol.server.dto.ApiDtos.EventsResponse;
import com.sistercontrol.server.exception.ApiException;
import com.sistercontrol.server.repository.ConnectionRepository;
import com.sistercontrol.server.repository.EventRepository;
import com.sistercontrol.server.security.AuthenticatedUser;
import org.springframework.stereotype.Service;

@Service
public class EventService {
    private final EventRepository events;
    private final ConnectionRepository connections;

    public EventService(EventRepository events, ConnectionRepository connections) {
        this.events = events;
        this.connections = connections;
    }

    public void createChildEvent(AuthenticatedUser user, EventRequest request) {
        requireChild(user);
        var parentId = connections.findParentIdByChild(user.id())
                .orElseThrow(() -> ApiException.badRequest("Child not paired"));
        events.create(parentId, user.id(), request.eventType(), request.message());
    }

    public EventsResponse parentEvents(AuthenticatedUser user, int limit) {
        requireParent(user);
        return new EventsResponse(events.findByParent(user.id(), clamp(limit, 1, 200)));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void requireParent(AuthenticatedUser user) {
        if (!user.isParent()) {
            throw ApiException.forbidden();
        }
    }

    private void requireChild(AuthenticatedUser user) {
        if (!user.isChild()) {
            throw ApiException.forbidden();
        }
    }
}

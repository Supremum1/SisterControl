package com.sistercontrol.server.service;

import com.sistercontrol.server.dto.ApiDtos.LocationRequest;
import com.sistercontrol.server.dto.ApiDtos.LocationResponse;
import com.sistercontrol.server.exception.ApiException;
import com.sistercontrol.server.repository.ConnectionRepository;
import com.sistercontrol.server.repository.EventRepository;
import com.sistercontrol.server.repository.LocationRepository;
import com.sistercontrol.server.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocationService {
    private final LocationRepository locations;
    private final ConnectionRepository connections;
    private final EventRepository events;

    public LocationService(LocationRepository locations, ConnectionRepository connections, EventRepository events) {
        this.locations = locations;
        this.connections = connections;
        this.events = events;
    }

    @Transactional
    public void upload(AuthenticatedUser user, LocationRequest request) {
        requireChild(user);
        var parentId = connections.findParentIdByChild(user.id())
                .orElseThrow(() -> ApiException.badRequest("Child not paired"));
        locations.upsert(user.id(), parentId, request.lat(), request.lon(), request.accuracy(), request.provider());
        events.create(parentId, user.id(), "GPS_LOCATION", "GPS updated: " + request.lat() + ", " + request.lon());
    }

    public LocationResponse currentChildLocation(AuthenticatedUser user) {
        requireParent(user);
        return locations.findLatestForParent(user.id()).orElseGet(LocationResponse::empty);
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

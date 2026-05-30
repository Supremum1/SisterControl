package com.sistercontrol.server.service;

import com.sistercontrol.server.dto.ApiDtos.CommandsResponse;
import com.sistercontrol.server.dto.ApiDtos.SafeZoneRequest;
import com.sistercontrol.server.exception.ApiException;
import com.sistercontrol.server.repository.CommandRepository;
import com.sistercontrol.server.repository.ConnectionRepository;
import com.sistercontrol.server.security.AuthenticatedUser;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class CommandService {
    private final CommandRepository commands;
    private final ConnectionRepository connections;

    public CommandService(CommandRepository commands, ConnectionRepository connections) {
        this.commands = commands;
        this.connections = connections;
    }

    public void requestGps(AuthenticatedUser user) {
        requireParent(user);
        UUID childId = connections.findChildIdByParent(user.id())
                .orElseThrow(() -> ApiException.badRequest("No child connected"));
        commands.create(childId, "REQUEST_GPS", Map.of("reason", "Parent requested GPS tracking"));
    }

    public void setSafeZone(AuthenticatedUser user, SafeZoneRequest request) {
        requireParent(user);
        if (request.radiusM() <= 0) {
            throw ApiException.badRequest("radiusM must be positive");
        }
        UUID childId = connections.findChildIdByParent(user.id())
                .orElseThrow(() -> ApiException.badRequest("No child connected"));
        commands.create(childId, "SET_SAFE_ZONE", Map.of(
                "lat", request.lat(),
                "lon", request.lon(),
                "radiusM", request.radiusM()
        ));
    }

    public CommandsResponse childCommands(AuthenticatedUser user, int limit) {
        requireChild(user);
        return new CommandsResponse(commands.findPending(user.id(), Math.max(1, Math.min(50, limit))));
    }

    public boolean markDone(AuthenticatedUser user, UUID commandId) {
        requireChild(user);
        return commands.markDone(user.id(), commandId);
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

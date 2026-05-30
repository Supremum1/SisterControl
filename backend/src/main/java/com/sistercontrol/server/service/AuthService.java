package com.sistercontrol.server.service;

import com.sistercontrol.server.config.SisterControlProperties;
import com.sistercontrol.server.dto.ApiDtos.AuthRequest;
import com.sistercontrol.server.dto.ApiDtos.AuthResponse;
import com.sistercontrol.server.dto.ApiDtos.LoginRequest;
import com.sistercontrol.server.dto.ApiDtos.UserDto;
import com.sistercontrol.server.exception.ApiException;
import com.sistercontrol.server.repository.*;
import com.sistercontrol.server.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {
    private static final Set<String> ROLES = Set.of("parent", "child");

    private final UserRepository users;
    private final SessionRepository sessions;
    private final EventRepository events;
    private final CommandRepository commands;
    private final LocationRepository locations;
    private final ScreenTimeRepository screenTime;
    private final PairingRepository pairing;
    private final ConnectionRepository connections;
    private final MessageRepository messages;
    private final PasswordEncoder passwordEncoder;
    private final SisterControlProperties properties;

    public AuthService(
            UserRepository users,
            SessionRepository sessions,
            EventRepository events,
            CommandRepository commands,
            LocationRepository locations,
            ScreenTimeRepository screenTime,
            PairingRepository pairing,
            ConnectionRepository connections,
            MessageRepository messages,
            PasswordEncoder passwordEncoder,
            SisterControlProperties properties
    ) {
        this.users = users;
        this.sessions = sessions;
        this.events = events;
        this.commands = commands;
        this.locations = locations;
        this.screenTime = screenTime;
        this.pairing = pairing;
        this.connections = connections;
        this.messages = messages;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Transactional
    public AuthResponse register(AuthRequest request) {
        String role = normalizeRole(request.role());
        UUID userId = UUID.randomUUID();
        users.create(userId, request.email(), passwordEncoder.encode(request.password()), role);
        return createSession(new UserDto(userId, request.email(), role));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserRepository.UserRecord user = users.findByEmail(request.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return createSession(user.toDto());
    }

    @Transactional
    public void deleteAccount(AuthenticatedUser user) {
        sessions.deleteByUserId(user.id());
        events.deleteByUser(user.id());
        commands.deleteByChild(user.id());
        locations.deleteByUser(user.id());
        screenTime.deleteByUser(user.id());
        pairing.deleteByChild(user.id());
        connections.deleteByUser(user.id());
        messages.deleteByUser(user.id());
        users.delete(user.id());
    }

    private AuthResponse createSession(UserDto user) {
        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(properties.security().sessionTtlDays(), ChronoUnit.DAYS);
        sessions.create(token, user.id(), expiresAt);
        return new AuthResponse(token, user);
    }

    private String normalizeRole(String role) {
        if (role == null || !ROLES.contains(role)) {
            throw ApiException.badRequest("role=parent|child required");
        }
        return role;
    }
}

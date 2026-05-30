package com.sistercontrol.server.service;

import com.sistercontrol.server.config.SisterControlProperties;
import com.sistercontrol.server.dto.ApiDtos.ChildConnectionResponse;
import com.sistercontrol.server.dto.ApiDtos.CodeResponse;
import com.sistercontrol.server.dto.ApiDtos.PairStatusResponse;
import com.sistercontrol.server.dto.ApiDtos.ParentConnectionResponse;
import com.sistercontrol.server.exception.ApiException;
import com.sistercontrol.server.repository.ConnectionRepository;
import com.sistercontrol.server.repository.PairingRepository;
import com.sistercontrol.server.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PairingService {
    private final PairingRepository pairing;
    private final ConnectionRepository connections;
    private final SisterControlProperties properties;

    public PairingService(
            PairingRepository pairing,
            ConnectionRepository connections,
            SisterControlProperties properties
    ) {
        this.pairing = pairing;
        this.connections = connections;
        this.properties = properties;
    }

    @Transactional
    public CodeResponse createCode(AuthenticatedUser user) {
        requireChild(user);
        String code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
        Instant expiresAt = Instant.now().plus(properties.security().pairCodeTtlMinutes(), ChronoUnit.MINUTES);
        pairing.replaceCodeForChild(user.id(), code, expiresAt);
        return new CodeResponse(code, expiresAt);
    }

    @Transactional
    public void pair(AuthenticatedUser user, String code) {
        requireParent(user);
        var pairCode = pairing.findCode(code).orElseThrow(() -> ApiException.notFound("Code not found"));
        if (pairCode.used()) {
            throw ApiException.badRequest("Code already used");
        }
        if (pairCode.expiresAt().isBefore(Instant.now())) {
            throw ApiException.badRequest("Code expired");
        }
        connections.createIfAbsent(user.id(), pairCode.childUserId());
        pairing.markUsed(code);
    }

    public PairStatusResponse childPairStatus(AuthenticatedUser user) {
        requireChild(user);
        return new PairStatusResponse(connections.childHasParent(user.id()));
    }

    public ParentConnectionResponse parentConnection(AuthenticatedUser user) {
        requireParent(user);
        return new ParentConnectionResponse(connections.findChildEmailByParent(user.id()).orElse(null));
    }

    public ChildConnectionResponse childConnection(AuthenticatedUser user) {
        requireChild(user);
        return new ChildConnectionResponse(connections.findParentEmailByChild(user.id()).orElse(null));
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

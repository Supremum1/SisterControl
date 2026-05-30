package com.sistercontrol.server.security;

import java.util.UUID;

public record AuthenticatedUser(UUID id, String email, String role, String token) {
    public boolean isParent() {
        return "parent".equals(role);
    }

    public boolean isChild() {
        return "child".equals(role);
    }
}

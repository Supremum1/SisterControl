package com.sistercontrol.server.repository;

import com.sistercontrol.server.security.AuthenticatedUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SessionRepository {
    private final JdbcTemplate jdbc;

    public SessionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void create(String token, UUID userId, Instant expiresAt) {
        jdbc.update("INSERT INTO sessions(token, user_id, expires_at) VALUES (?, ?, ?)", token, userId, Timestamp.from(expiresAt));
    }

    public Optional<AuthenticatedUser> findActiveUserByToken(String token) {
        return jdbc.query(
                """
                SELECT u.id, u.email, u.role
                FROM sessions s
                JOIN users u ON u.id = s.user_id
                WHERE s.token = ? AND s.expires_at > NOW()
                """,
                (rs, rowNum) -> new AuthenticatedUser(
                        rs.getObject("id", UUID.class),
                        rs.getString("email"),
                        rs.getString("role"),
                        token
                ),
                token
        ).stream().findFirst();
    }

    public void deleteByUserId(UUID userId) {
        jdbc.update("DELETE FROM sessions WHERE user_id = ?", userId);
    }
}

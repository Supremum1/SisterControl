package com.sistercontrol.server.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PairingRepository {
    private final JdbcTemplate jdbc;

    public PairingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void replaceCodeForChild(UUID childId, String code, Instant expiresAt) {
        jdbc.update("DELETE FROM pairing_codes WHERE child_user_id = ? OR expires_at < NOW()", childId);
        jdbc.update(
                "INSERT INTO pairing_codes(code, child_user_id, expires_at) VALUES (?, ?, ?)",
                code, childId, Timestamp.from(expiresAt)
        );
    }

    public Optional<PairingCodeRecord> findCode(String code) {
        return jdbc.query(
                "SELECT code, child_user_id, expires_at, used FROM pairing_codes WHERE code = ?",
                (rs, rowNum) -> new PairingCodeRecord(
                        rs.getString("code"),
                        rs.getObject("child_user_id", UUID.class),
                        rs.getTimestamp("expires_at").toInstant(),
                        rs.getBoolean("used")
                ),
                code
        ).stream().findFirst();
    }

    public void markUsed(String code) {
        jdbc.update("UPDATE pairing_codes SET used = TRUE WHERE code = ?", code);
    }

    public void deleteByChild(UUID childId) {
        jdbc.update("DELETE FROM pairing_codes WHERE child_user_id = ?", childId);
    }

    public record PairingCodeRecord(String code, UUID childUserId, Instant expiresAt, boolean used) {
    }
}

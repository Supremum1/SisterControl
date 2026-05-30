package com.sistercontrol.server.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistercontrol.server.dto.ApiDtos.CommandDto;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class CommandRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public CommandRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public void create(UUID childId, String type, Map<String, Object> payload) {
        jdbc.update(
                "INSERT INTO child_commands(id, child_user_id, command_type, payload) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), childId, type, jsonb(payload)
        );
    }

    public List<CommandDto> findPending(UUID childId, int limit) {
        return jdbc.query(
                """
                SELECT id, command_type, payload, created_at
                FROM child_commands
                WHERE child_user_id = ? AND status = 'pending'
                ORDER BY created_at ASC
                LIMIT ?
                """,
                (rs, rowNum) -> new CommandDto(
                        rs.getObject("id", UUID.class),
                        rs.getString("command_type"),
                        parsePayload(rs.getString("payload")),
                        rs.getTimestamp("created_at").toInstant()
                ),
                childId, limit
        );
    }

    public boolean markDone(UUID childId, UUID commandId) {
        int updated = jdbc.update(
                "UPDATE child_commands SET status = 'done' WHERE id = ? AND child_user_id = ?",
                commandId, childId
        );
        return updated > 0;
    }

    public void deleteByChild(UUID childId) {
        jdbc.update("DELETE FROM child_commands WHERE child_user_id = ?", childId);
    }

    private PGobject jsonb(Map<String, Object> payload) {
        try {
            PGobject object = new PGobject();
            object.setType("jsonb");
            object.setValue(objectMapper.writeValueAsString(payload));
            return object;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Cannot serialize command payload", ex);
        }
    }

    private Map<String, Object> parsePayload(String payload) throws SQLException {
        if (payload == null || payload.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(payload, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new SQLException("Cannot parse command payload", ex);
        }
    }
}

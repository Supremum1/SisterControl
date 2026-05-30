package com.sistercontrol.server.repository;

import com.sistercontrol.server.dto.ApiDtos.EventDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class EventRepository {
    private final JdbcTemplate jdbc;

    public EventRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void create(UUID parentId, UUID childId, String eventType, String message) {
        jdbc.update(
                "INSERT INTO child_events(id, parent_user_id, child_user_id, event_type, message) VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(), parentId, childId, eventType, message
        );
    }

    public List<EventDto> findByParent(UUID parentId, int limit) {
        return jdbc.query(
                """
                SELECT event_type, message, created_at
                FROM child_events
                WHERE parent_user_id = ?
                ORDER BY created_at DESC
                LIMIT ?
                """,
                (rs, rowNum) -> new EventDto(
                        rs.getString("event_type"),
                        rs.getString("message"),
                        rs.getTimestamp("created_at").toInstant()
                ),
                parentId, limit
        );
    }

    public void deleteByUser(UUID userId) {
        jdbc.update("DELETE FROM child_events WHERE parent_user_id = ? OR child_user_id = ?", userId, userId);
    }
}

package com.sistercontrol.server.repository;

import com.sistercontrol.server.dto.ApiDtos.ParentMessageDto;
import com.sistercontrol.server.dto.ApiDtos.TrustLetterDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class MessageRepository {
    private final JdbcTemplate jdbc;

    public MessageRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void createTrustLetter(UUID parentId, UUID childId, String message) {
        jdbc.update(
                "INSERT INTO trust_letters(id, parent_user_id, child_user_id, message) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), parentId, childId, message
        );
    }

    public List<TrustLetterDto> findTrustLetters(UUID parentId, int limit) {
        return jdbc.query(
                """
                SELECT id, message, created_at, is_read
                FROM trust_letters
                WHERE parent_user_id = ?
                ORDER BY created_at DESC
                LIMIT ?
                """,
                (rs, rowNum) -> new TrustLetterDto(
                        rs.getObject("id", UUID.class),
                        rs.getString("message"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getBoolean("is_read")
                ),
                parentId, limit
        );
    }

    public void createParentMessage(UUID parentId, UUID childId, String message) {
        jdbc.update(
                "INSERT INTO parent_messages(id, parent_user_id, child_user_id, message) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), parentId, childId, message
        );
    }

    public List<ParentMessageDto> findParentMessages(UUID childId, int limit) {
        return jdbc.query(
                """
                SELECT id, message, created_at, is_read
                FROM parent_messages
                WHERE child_user_id = ?
                ORDER BY created_at DESC
                LIMIT ?
                """,
                (rs, rowNum) -> new ParentMessageDto(
                        rs.getObject("id", UUID.class),
                        rs.getString("message"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getBoolean("is_read")
                ),
                childId, limit
        );
    }

    public void deleteByUser(UUID userId) {
        jdbc.update("DELETE FROM trust_letters WHERE parent_user_id = ? OR child_user_id = ?", userId, userId);
        jdbc.update("DELETE FROM parent_messages WHERE parent_user_id = ? OR child_user_id = ?", userId, userId);
    }
}

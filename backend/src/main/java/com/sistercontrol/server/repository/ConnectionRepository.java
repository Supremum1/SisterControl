package com.sistercontrol.server.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class ConnectionRepository {
    private final JdbcTemplate jdbc;

    public ConnectionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean childHasParent(UUID childId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM connections WHERE child_user_id = ?",
                Integer.class,
                childId
        );
        return count != null && count > 0;
    }

    public void createIfAbsent(UUID parentId, UUID childId) {
        if (!childHasParent(childId)) {
            jdbc.update(
                    "INSERT INTO connections(id, parent_user_id, child_user_id) VALUES (?, ?, ?)",
                    UUID.randomUUID(), parentId, childId
            );
        }
    }

    public Optional<UUID> findChildIdByParent(UUID parentId) {
        return jdbc.query(
                "SELECT child_user_id FROM connections WHERE parent_user_id = ? LIMIT 1",
                (rs, rowNum) -> rs.getObject("child_user_id", UUID.class),
                parentId
        ).stream().findFirst();
    }

    public Optional<UUID> findParentIdByChild(UUID childId) {
        return jdbc.query(
                "SELECT parent_user_id FROM connections WHERE child_user_id = ? LIMIT 1",
                (rs, rowNum) -> rs.getObject("parent_user_id", UUID.class),
                childId
        ).stream().findFirst();
    }

    public Optional<String> findChildEmailByParent(UUID parentId) {
        return jdbc.query(
                """
                SELECT u.email AS child_email
                FROM connections c
                JOIN users u ON u.id = c.child_user_id
                WHERE c.parent_user_id = ?
                LIMIT 1
                """,
                (rs, rowNum) -> rs.getString("child_email"),
                parentId
        ).stream().findFirst();
    }

    public Optional<String> findParentEmailByChild(UUID childId) {
        return jdbc.query(
                """
                SELECT u.email AS parent_email
                FROM connections c
                JOIN users u ON u.id = c.parent_user_id
                WHERE c.child_user_id = ?
                LIMIT 1
                """,
                (rs, rowNum) -> rs.getString("parent_email"),
                childId
        ).stream().findFirst();
    }

    public void deleteByUser(UUID userId) {
        jdbc.update("DELETE FROM connections WHERE parent_user_id = ? OR child_user_id = ?", userId, userId);
    }
}

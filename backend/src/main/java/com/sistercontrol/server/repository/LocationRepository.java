package com.sistercontrol.server.repository;

import com.sistercontrol.server.dto.ApiDtos.LocationResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class LocationRepository {
    private final JdbcTemplate jdbc;

    public LocationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void upsert(UUID childId, UUID parentId, double lat, double lon, Double accuracy, String provider) {
        jdbc.update(
                """
                INSERT INTO child_locations(child_user_id, parent_user_id, lat, lon, accuracy, provider, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, NOW())
                ON CONFLICT (child_user_id)
                DO UPDATE SET parent_user_id = EXCLUDED.parent_user_id,
                              lat = EXCLUDED.lat,
                              lon = EXCLUDED.lon,
                              accuracy = EXCLUDED.accuracy,
                              provider = EXCLUDED.provider,
                              updated_at = NOW()
                """,
                childId, parentId, lat, lon, accuracy, provider
        );
    }

    public Optional<LocationResponse> findLatestForParent(UUID parentId) {
        return jdbc.query(
                """
                SELECT lat, lon, accuracy, provider, updated_at
                FROM child_locations
                WHERE parent_user_id = ?
                ORDER BY updated_at DESC
                LIMIT 1
                """,
                (rs, rowNum) -> new LocationResponse(
                        true,
                        rs.getDouble("lat"),
                        rs.getDouble("lon"),
                        (Double) rs.getObject("accuracy"),
                        rs.getString("provider"),
                        rs.getTimestamp("updated_at").toInstant()
                ),
                parentId
        ).stream().findFirst();
    }

    public void deleteByUser(UUID userId) {
        jdbc.update("DELETE FROM child_locations WHERE parent_user_id = ? OR child_user_id = ?", userId, userId);
    }
}

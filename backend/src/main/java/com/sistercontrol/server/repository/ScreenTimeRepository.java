package com.sistercontrol.server.repository;

import com.sistercontrol.server.dto.ApiDtos.ParentUsageDto;
import com.sistercontrol.server.dto.ApiDtos.ScreenTimeRuleDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ScreenTimeRepository {
    private final JdbcTemplate jdbc;

    public ScreenTimeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void savePolicy(UUID parentId, UUID childId, int totalLimitMin, List<ScreenTimeRuleDto> rules) {
        UUID policyId = findPolicyIdByChild(childId).orElseGet(UUID::randomUUID);
        if (findPolicyIdByChild(childId).isPresent()) {
            jdbc.update(
                    "UPDATE screen_time_policies SET total_limit_min = ?, updated_at = NOW() WHERE id = ?",
                    Math.max(totalLimitMin, 0), policyId
            );
            jdbc.update("DELETE FROM screen_time_rules WHERE policy_id = ?", policyId);
        } else {
            jdbc.update(
                    "INSERT INTO screen_time_policies(id, parent_user_id, child_user_id, total_limit_min) VALUES (?, ?, ?, ?)",
                    policyId, parentId, childId, Math.max(totalLimitMin, 0)
            );
        }

        if (rules == null) {
            return;
        }
        for (ScreenTimeRuleDto rule : rules) {
            if (rule == null || rule.packageName() == null || rule.packageName().isBlank() || rule.limitMin() <= 0) {
                continue;
            }
            jdbc.update(
                    "INSERT INTO screen_time_rules(id, policy_id, package_name, limit_min) VALUES (?, ?, ?, ?)",
                    UUID.randomUUID(), policyId, rule.packageName().trim(), rule.limitMin()
            );
        }
    }

    public Optional<PolicyRecord> findPolicyByChild(UUID childId) {
        return jdbc.query(
                "SELECT id, total_limit_min FROM screen_time_policies WHERE child_user_id = ? LIMIT 1",
                (rs, rowNum) -> new PolicyRecord(
                        rs.getObject("id", UUID.class),
                        rs.getInt("total_limit_min")
                ),
                childId
        ).stream().findFirst();
    }

    public List<ScreenTimeRuleDto> findRules(UUID policyId) {
        return jdbc.query(
                "SELECT package_name, limit_min FROM screen_time_rules WHERE policy_id = ? ORDER BY package_name ASC",
                (rs, rowNum) -> new ScreenTimeRuleDto(rs.getString("package_name"), rs.getInt("limit_min")),
                policyId
        );
    }

    public void upsertUsage(UUID childId, LocalDate day, String packageName, int usedSec) {
        jdbc.update(
                """
                INSERT INTO screen_time_usage(id, child_user_id, day, package_name, used_sec)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(child_user_id, day, package_name)
                DO UPDATE SET used_sec = EXCLUDED.used_sec, updated_at = NOW()
                """,
                UUID.randomUUID(), childId, day, packageName, Math.max(usedSec, 0)
        );
    }

    public List<ParentUsageDto> findUsageForParent(UUID childId, UUID policyId, LocalDate day) {
        return jdbc.query(
                """
                SELECT r.package_name,
                       r.limit_min,
                       COALESCE(u.used_sec, 0) AS used_sec
                FROM screen_time_rules r
                LEFT JOIN screen_time_usage u
                  ON u.child_user_id = ?
                 AND u.day = ?
                 AND u.package_name = r.package_name
                WHERE r.policy_id = ?
                ORDER BY r.package_name ASC
                """,
                (rs, rowNum) -> new ParentUsageDto(
                        rs.getString("package_name"),
                        rs.getInt("limit_min"),
                        rs.getInt("used_sec")
                ),
                childId, day, policyId
        );
    }

    public void deleteByUser(UUID userId) {
        jdbc.update("DELETE FROM screen_time_usage WHERE child_user_id = ?", userId);
        jdbc.update("DELETE FROM screen_time_policies WHERE parent_user_id = ? OR child_user_id = ?", userId, userId);
    }

    private Optional<UUID> findPolicyIdByChild(UUID childId) {
        return jdbc.query(
                "SELECT id FROM screen_time_policies WHERE child_user_id = ? LIMIT 1",
                (rs, rowNum) -> rs.getObject("id", UUID.class),
                childId
        ).stream().findFirst();
    }

    public record PolicyRecord(UUID id, int totalLimitMin) {
    }
}

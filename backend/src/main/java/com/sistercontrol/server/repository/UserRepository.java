package com.sistercontrol.server.repository;

import com.sistercontrol.server.dto.ApiDtos.UserDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void create(UUID id, String email, String passwordHash, String role) {
        jdbc.update(
                "INSERT INTO users(id, email, password_hash, role) VALUES (?, ?, ?, ?)",
                id, email, passwordHash, role
        );
    }

    public Optional<UserRecord> findByEmail(String email) {
        return jdbc.query(
                "SELECT id, email, password_hash, role FROM users WHERE email = ?",
                (rs, rowNum) -> new UserRecord(
                        rs.getObject("id", UUID.class),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("role")
                ),
                email
        ).stream().findFirst();
    }

    public Optional<UserDto> findDtoById(UUID id) {
        return jdbc.query(
                "SELECT id, email, role FROM users WHERE id = ?",
                (rs, rowNum) -> new UserDto(
                        rs.getObject("id", UUID.class),
                        rs.getString("email"),
                        rs.getString("role")
                ),
                id
        ).stream().findFirst();
    }

    public void delete(UUID id) {
        jdbc.update("DELETE FROM users WHERE id = ?", id);
    }

    public record UserRecord(UUID id, String email, String passwordHash, String role) {
        public UserDto toDto() {
            return new UserDto(id, email, role);
        }
    }
}

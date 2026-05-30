package com.sistercontrol.server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ApiDtos {
    private ApiDtos() {
    }

    public record UserDto(UUID id, String email, String role) {
    }

    public record AuthRequest(@Email @NotBlank String email, @NotBlank String password, String role) {
        public AuthRequest {
            email = normalizeEmail(email);
        }
    }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {
        public LoginRequest {
            email = normalizeEmail(email);
        }
    }

    public record AuthResponse(String token, UserDto user) {
    }

    public record CodeResponse(String code, Instant expiresAt) {
    }

    public record PairRequest(@NotBlank String code) {
    }

    public record PairStatusResponse(boolean connected) {
    }

    public record ParentConnectionResponse(String childEmail) {
    }

    public record ChildConnectionResponse(String parentEmail) {
    }

    public record EventRequest(@NotBlank String eventType, @NotBlank String message) {
    }

    public record EventDto(String eventType, String message, Instant createdAt) {
    }

    public record EventsResponse(List<EventDto> events) {
    }

    public record CommandDto(UUID id, String commandType, Map<String, Object> payload, Instant createdAt) {
    }

    public record CommandsResponse(List<CommandDto> commands) {
    }

    public record LocationRequest(@NotNull Double lat, @NotNull Double lon, Double accuracy, String provider) {
    }

    public record LocationResponse(
            boolean hasLocation,
            Double lat,
            Double lon,
            Double accuracy,
            String provider,
            Instant updatedAt
    ) {
        public static LocationResponse empty() {
            return new LocationResponse(false, null, null, null, null, null);
        }
    }

    public record SafeZoneRequest(@NotNull Double lat, @NotNull Double lon, @NotNull Double radiusM) {
    }

    public record ScreenTimeRuleDto(String packageName, int limitMin) {
    }

    public record ScreenTimePolicyRequest(int totalLimitMin, List<ScreenTimeRuleDto> rules) {
    }

    public record ScreenTimePolicyResponse(int totalLimitMin, List<ScreenTimeRuleDto> rules) {
    }

    public record UsageDto(String packageName, int usedSec) {
    }

    public record UsageUploadRequest(@NotNull LocalDate day, List<UsageDto> usage) {
    }

    public record ParentUsageDto(String packageName, int limitMin, int usedSec) {
    }

    public record ParentUsageResponse(LocalDate day, List<ParentUsageDto> apps) {
    }

    public record TextMessageRequest(@NotBlank String message) {
    }

    public record TrustLetterDto(UUID id, String message, Instant createdAt, boolean isRead) {
    }

    public record TrustLettersResponse(List<TrustLetterDto> letters) {
    }

    public record ParentMessageDto(UUID id, String message, Instant createdAt, boolean isRead) {
    }

    public record ParentMessagesResponse(List<ParentMessageDto> messages) {
    }

    public record ChatMessage(String role, String content) {
    }

    public record AiChatRequest(String childName, List<ChatMessage> messages) {
    }

    public record AiChatResponse(String answer) {
    }

    public record OkResponse(boolean ok) {
        public static OkResponse success() {
            return new OkResponse(true);
        }
    }

    public record ErrorResponse(String error, String detail) {
        public ErrorResponse(String error) {
            this(error, null);
        }
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}

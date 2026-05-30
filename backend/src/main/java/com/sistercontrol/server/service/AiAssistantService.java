package com.sistercontrol.server.service;

import com.sistercontrol.server.config.SisterControlProperties;
import com.sistercontrol.server.dto.ApiDtos.AiChatRequest;
import com.sistercontrol.server.dto.ApiDtos.AiChatResponse;
import com.sistercontrol.server.dto.ApiDtos.ChatMessage;
import com.sistercontrol.server.exception.ApiException;
import com.sistercontrol.server.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiAssistantService {
    private final WebClient ollamaClient;
    private final SisterControlProperties properties;

    public AiAssistantService(WebClient.Builder webClientBuilder, SisterControlProperties properties) {
        this.ollamaClient = webClientBuilder.baseUrl(properties.ollama().baseUrl()).build();
        this.properties = properties;
    }

    public AiChatResponse chat(AuthenticatedUser user, AiChatRequest request) {
        if (!user.isChild()) {
            throw ApiException.forbidden();
        }
        if (request.messages() == null) {
            throw ApiException.badRequest("messages must be array");
        }

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", buildSystemPrompt(request.childName())));
        for (ChatMessage message : request.messages()) {
            String role = "assistant".equals(message.role()) ? "assistant" : "user";
            messages.add(Map.of("role", role, "content", String.valueOf(message.content())));
        }

        Map<?, ?> response = ollamaClient.post()
                .uri("/api/chat")
                .bodyValue(Map.of(
                        "model", properties.ollama().model(),
                        "messages", messages,
                        "stream", false,
                        "options", Map.of("temperature", 0.6)
                ))
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), clientResponse -> clientResponse.bodyToMono(String.class)
                        .map(body -> new ApiException(HttpStatus.BAD_GATEWAY, "ollama_error: " + body)))
                .bodyToMono(Map.class)
                .block();

        Object message = response == null ? null : response.get("message");
        String answer = "";
        if (message instanceof Map<?, ?> messageMap) {
            Object content = messageMap.get("content");
            answer = content == null ? "" : content.toString().trim();
        }
        return new AiChatResponse(answer);
    }

    private String buildSystemPrompt(String childName) {
        String name = childName == null || childName.isBlank() ? "" : "Ребёнка зовут " + childName + ". ";
        return "Ты — доброжелательный психологический помощник для ребенка. "
                + name
                + "Твоя задача: поддержать, помочь разобрать ситуацию, предложить безопасные шаги: "
                + "как справиться с грустью, буллингом, конфликтом с другом, стрессом, тревогой. "
                + "Говори простыми словами, без морализаторства, 5–10 предложений, можно списком. "
                + "Не давай медицинских диагнозов и не назначай лекарства. "
                + "Если ребёнок говорит, что ему может быть небезопасно или что он может навредить себе или кому-то, "
                + "скажи, что нужно срочно обратиться к доверенному взрослому и не оставаться одному.";
    }
}

package com.sistercontrol.server.controller;

import com.sistercontrol.server.dto.ApiDtos.AiChatRequest;
import com.sistercontrol.server.dto.ApiDtos.AiChatResponse;
import com.sistercontrol.server.security.CurrentUser;
import com.sistercontrol.server.service.AiAssistantService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/child/ai")
public class AiController {
    private final AiAssistantService aiAssistantService;
    private final CurrentUser currentUser;

    public AiController(AiAssistantService aiAssistantService, CurrentUser currentUser) {
        this.aiAssistantService = aiAssistantService;
        this.currentUser = currentUser;
    }

    @PostMapping("/chat")
    @PreAuthorize("hasRole('CHILD')")
    public AiChatResponse chat(@RequestBody AiChatRequest request) {
        return aiAssistantService.chat(currentUser.get(), request);
    }
}

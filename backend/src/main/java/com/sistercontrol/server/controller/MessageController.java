package com.sistercontrol.server.controller;

import com.sistercontrol.server.dto.ApiDtos.*;
import com.sistercontrol.server.security.CurrentUser;
import com.sistercontrol.server.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class MessageController {
    private final MessageService messageService;
    private final CurrentUser currentUser;

    public MessageController(MessageService messageService, CurrentUser currentUser) {
        this.messageService = messageService;
        this.currentUser = currentUser;
    }

    @PostMapping("/child/trust-letter")
    @PreAuthorize("hasRole('CHILD')")
    public OkResponse createTrustLetter(@Valid @RequestBody TextMessageRequest request) {
        messageService.createTrustLetter(currentUser.get(), request);
        return OkResponse.success();
    }

    @GetMapping("/parent/trust-letters")
    @PreAuthorize("hasRole('PARENT')")
    public TrustLettersResponse trustLetters(@RequestParam(defaultValue = "50") int limit) {
        return messageService.trustLetters(currentUser.get(), limit);
    }

    @PostMapping("/parent/message-to-child")
    @PreAuthorize("hasRole('PARENT')")
    public OkResponse messageToChild(@Valid @RequestBody TextMessageRequest request) {
        messageService.createParentMessage(currentUser.get(), request);
        return OkResponse.success();
    }

    @GetMapping("/child/parent-messages")
    @PreAuthorize("hasRole('CHILD')")
    public ParentMessagesResponse parentMessages(@RequestParam(defaultValue = "50") int limit) {
        return messageService.parentMessages(currentUser.get(), limit);
    }
}

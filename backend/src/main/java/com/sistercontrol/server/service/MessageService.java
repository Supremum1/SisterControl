package com.sistercontrol.server.service;

import com.sistercontrol.server.dto.ApiDtos.ParentMessagesResponse;
import com.sistercontrol.server.dto.ApiDtos.TextMessageRequest;
import com.sistercontrol.server.dto.ApiDtos.TrustLettersResponse;
import com.sistercontrol.server.exception.ApiException;
import com.sistercontrol.server.repository.ConnectionRepository;
import com.sistercontrol.server.repository.MessageRepository;
import com.sistercontrol.server.security.AuthenticatedUser;
import org.springframework.stereotype.Service;

@Service
public class MessageService {
    private final MessageRepository messages;
    private final ConnectionRepository connections;

    public MessageService(MessageRepository messages, ConnectionRepository connections) {
        this.messages = messages;
        this.connections = connections;
    }

    public void createTrustLetter(AuthenticatedUser user, TextMessageRequest request) {
        requireChild(user);
        String message = cleanMessage(request.message());
        var parentId = connections.findParentIdByChild(user.id())
                .orElseThrow(() -> ApiException.badRequest("Child not paired"));
        messages.createTrustLetter(parentId, user.id(), message);
    }

    public TrustLettersResponse trustLetters(AuthenticatedUser user, int limit) {
        requireParent(user);
        return new TrustLettersResponse(messages.findTrustLetters(user.id(), clamp(limit)));
    }

    public void createParentMessage(AuthenticatedUser user, TextMessageRequest request) {
        requireParent(user);
        String message = cleanMessage(request.message());
        var childId = connections.findChildIdByParent(user.id())
                .orElseThrow(() -> ApiException.badRequest("No child connected"));
        messages.createParentMessage(user.id(), childId, message);
    }

    public ParentMessagesResponse parentMessages(AuthenticatedUser user, int limit) {
        requireChild(user);
        return new ParentMessagesResponse(messages.findParentMessages(user.id(), clamp(limit)));
    }

    private String cleanMessage(String message) {
        String clean = message == null ? "" : message.trim();
        if (clean.isEmpty()) {
            throw ApiException.badRequest("message required");
        }
        if (clean.length() > 4000) {
            throw ApiException.badRequest("message too long (max 4000)");
        }
        return clean;
    }

    private int clamp(int limit) {
        return Math.max(1, Math.min(200, limit));
    }

    private void requireParent(AuthenticatedUser user) {
        if (!user.isParent()) {
            throw ApiException.forbidden();
        }
    }

    private void requireChild(AuthenticatedUser user) {
        if (!user.isChild()) {
            throw ApiException.forbidden();
        }
    }
}

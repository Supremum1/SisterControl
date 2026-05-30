package com.example.childassistant.net;

import java.util.List;

public class AiChatRequest {
    private String childName;
    private List<AiMessage> messages;

    public AiChatRequest(String childName, List<AiMessage> messages) {
        this.childName = childName;
        this.messages = messages;
    }

    public String getChildName() {
        return childName;
    }

    public void setChildName(String childName) {
        this.childName = childName;
    }

    public List<AiMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<AiMessage> messages) {
        this.messages = messages;
    }
}

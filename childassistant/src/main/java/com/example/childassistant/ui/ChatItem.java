package com.example.childassistant.ui;

public class ChatItem {
    private final boolean user;
    private final String text;

    public ChatItem(boolean user, String text) {
        this.user = user;
        this.text = text;
    }

    public boolean isUser() {
        return user;
    }

    public String getText() {
        return text;
    }
}

package com.example.childapp;

import com.google.gson.annotations.SerializedName;

import java.util.List;

class User {
    private String id;
    private String email;
    private String role;

    public User() {
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }
}

class ChildConnectionInfo {
    private String parentEmail;

    public ChildConnectionInfo() {
    }

    public String getParentEmail() {
        return parentEmail;
    }
}

class RegisterRequest {
    private String email;
    private String password;
    private String role;

    public RegisterRequest(String email, String password, String role) {
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }
}

class LoginRequest {
    private String email;
    private String password;

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}

class AuthResponse {
    private String token;
    private User user;

    public AuthResponse() {
    }

    public String getToken() {
        return token;
    }

    public User getUser() {
        return user;
    }
}

class CreateCodeResponse {
    private String code;
    private String expiresAt;

    public CreateCodeResponse() {
    }

    public String getCode() {
        return code;
    }

    public String getExpiresAt() {
        return expiresAt;
    }
}

class PairStatusResponse {
    private boolean connected;

    public PairStatusResponse() {
    }

    public boolean getConnected() {
        return connected;
    }
}

class ChildEventRequest {
    private String eventType;
    private String message;

    public ChildEventRequest(String eventType, String message) {
        this.eventType = eventType;
        this.message = message;
    }

    public String getEventType() {
        return eventType;
    }

    public String getMessage() {
        return message;
    }
}

class CommandItem {
    private String id;
    @SerializedName("command_type")
    private String commandType;
    private Object payload;
    @SerializedName("created_at")
    private String createdAt;

    public CommandItem() {
    }

    public String getId() {
        return id;
    }

    public String getCommandType() {
        return commandType;
    }

    public Object getPayload() {
        return payload;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}

class CommandsResponse {
    private List<CommandItem> commands;

    public CommandsResponse() {
    }

    public List<CommandItem> getCommands() {
        return commands == null ? List.of() : commands;
    }
}

class LocationUpdateRequest {
    private double lat;
    private double lon;
    private Double accuracy;
    private String provider;

    public LocationUpdateRequest(double lat, double lon, Double accuracy, String provider) {
        this.lat = lat;
        this.lon = lon;
        this.accuracy = accuracy;
        this.provider = provider;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public String getProvider() {
        return provider;
    }
}

class BasicResponse {
    private boolean ok;

    public BasicResponse() {
    }

    public boolean getOk() {
        return ok;
    }
}

class TrustLetterRequest {
    private String message;

    public TrustLetterRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

class TrustLetterResponse {
    private boolean ok;

    public TrustLetterResponse() {
    }

    public boolean getOk() {
        return ok;
    }
}

class TrustLetterItem {
    private String id;
    private String message;
    private String createdAt;
    private boolean isRead;

    public TrustLetterItem() {
    }

    public String getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public boolean isRead() {
        return isRead;
    }
}

class TrustLettersResponse {
    private List<TrustLetterItem> letters;

    public TrustLettersResponse() {
    }

    public List<TrustLetterItem> getLetters() {
        return letters == null ? List.of() : letters;
    }
}

class ParentMsgItem {
    private String id;
    private String message;
    private String createdAt;
    private boolean isRead;

    public ParentMsgItem() {
    }

    public String getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public boolean isRead() {
        return isRead;
    }
}

class ParentMessagesResponse {
    private List<ParentMsgItem> messages;

    public ParentMessagesResponse() {
    }

    public List<ParentMsgItem> getMessages() {
        return messages == null ? List.of() : messages;
    }
}

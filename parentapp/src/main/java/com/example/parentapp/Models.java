package com.example.parentapp;

import java.util.List;

class User {
    private String id;
    private String email;
    private String role;

    public User() {
    }

    public User(String id, String email, String role) {
        this.id = id;
        this.email = email;
        this.role = role;
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

class PairRequest {
    private String code;

    public PairRequest(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
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

class ParentConnectionsResponse {
    private String childEmail;

    public ParentConnectionsResponse() {
    }

    public String getChildEmail() {
        return childEmail;
    }
}

class EventEntry {
    private String event_type;
    private String message;
    private String created_at;

    public EventEntry() {
    }

    public String getEventType() {
        return event_type;
    }

    public String getMessage() {
        return message;
    }

    public String getCreatedAt() {
        return created_at;
    }
}

class EventsResponse {
    private List<EventEntry> events;

    public EventsResponse() {
    }

    public List<EventEntry> getEvents() {
        return events;
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

class ChildLocationResponse {
    private boolean hasLocation;
    private Double lat;
    private Double lon;
    private Double accuracy;
    private String provider;
    private String updated_at;

    public ChildLocationResponse() {
    }

    public boolean getHasLocation() {
        return hasLocation;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLon() {
        return lon;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public String getProvider() {
        return provider;
    }

    public String getUpdatedAt() {
        return updated_at;
    }
}

class SafeZoneRequest {
    private double lat;
    private double lon;
    private double radiusM;

    public SafeZoneRequest(double lat, double lon, double radiusM) {
        this.lat = lat;
        this.lon = lon;
        this.radiusM = radiusM;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public double getRadiusM() {
        return radiusM;
    }
}

class ScreenTimeRule {
    private String packageName;
    private int limitMin;

    public ScreenTimeRule() {
    }

    public ScreenTimeRule(String packageName, int limitMin) {
        this.packageName = packageName;
        this.limitMin = limitMin;
    }

    public String getPackageName() {
        return packageName;
    }

    public int getLimitMin() {
        return limitMin;
    }
}

class ScreenTimePolicyRequest {
    private int totalLimitMin;
    private List<ScreenTimeRule> rules;

    public ScreenTimePolicyRequest(int totalLimitMin, List<ScreenTimeRule> rules) {
        this.totalLimitMin = totalLimitMin;
        this.rules = rules;
    }

    public int getTotalLimitMin() {
        return totalLimitMin;
    }

    public List<ScreenTimeRule> getRules() {
        return rules;
    }
}

class ScreenTimePolicyResponse {
    private int totalLimitMin;
    private List<ScreenTimeRule> rules;

    public ScreenTimePolicyResponse() {
    }

    public int getTotalLimitMin() {
        return totalLimitMin;
    }

    public List<ScreenTimeRule> getRules() {
        return rules == null ? List.of() : rules;
    }
}

class ScreenTimeUsageItem {
    private String packageName;
    private int limitMin;
    private int usedSec;

    public ScreenTimeUsageItem() {
    }

    public String getPackageName() {
        return packageName;
    }

    public int getLimitMin() {
        return limitMin;
    }

    public int getUsedSec() {
        return usedSec;
    }
}

class ScreenTimeUsageResponse {
    private String day;
    private List<ScreenTimeUsageItem> apps;

    public ScreenTimeUsageResponse() {
    }

    public String getDay() {
        return day;
    }

    public List<ScreenTimeUsageItem> getApps() {
        return apps == null ? List.of() : apps;
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

class ParentMessageToChildRequest {
    private String message;

    public ParentMessageToChildRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

class ParentMessageToChildResponse {
    private boolean ok;

    public ParentMessageToChildResponse() {
    }

    public boolean getOk() {
        return ok;
    }
}

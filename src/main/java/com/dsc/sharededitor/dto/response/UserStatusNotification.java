package com.dsc.sharededitor.dto.response;

public class UserStatusNotification {
    private final String type = "USER_STATUS";
    private String username;
    private String status;   // "JOINED" | "LEFT"
    private String message;

    public UserStatusNotification() {}
    public UserStatusNotification(String username, String status, String message) {
        this.username = username; this.status = status; this.message = message;
    }

    public String getType()    { return type; }
    public String getUsername(){ return username; }
    public String getStatus()  { return status; }
    public String getMessage() { return message; }
    public void setUsername(String username) { this.username = username; }
    public void setStatus(String status)     { this.status = status; }
    public void setMessage(String message)   { this.message = message; }
}

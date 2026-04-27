package com.dsc.sharededitor.dto.response;

public class LoginResponse {
    private final String type = "LOGIN_RESPONSE";
    private boolean success;
    private String username;
    private String message;

    public LoginResponse() {}
    private LoginResponse(boolean success, String username, String message) {
        this.success = success; this.username = username; this.message = message;
    }

    public static LoginResponse success(String username) {
        return new LoginResponse(true, username, "로그인 성공");
    }
    public static LoginResponse fail(String message) {
        return new LoginResponse(false, null, message);
    }

    public String getType()    { return type; }
    public boolean isSuccess() { return success; }
    public String getUsername(){ return username; }
    public String getMessage() { return message; }
    public void setSuccess(boolean success)   { this.success = success; }
    public void setUsername(String username)  { this.username = username; }
    public void setMessage(String message)    { this.message = message; }
}

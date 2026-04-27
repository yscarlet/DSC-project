package com.dsc.sharededitor.dto.response;

public class InviteUserResponse {
    private final String type = "INVITE_USER_RESPONSE";
    private boolean success;
    private String message;

    public InviteUserResponse() {}
    private InviteUserResponse(boolean success, String message) {
        this.success = success; this.message = message;
    }

    public static InviteUserResponse success(String targetUsername) {
        return new InviteUserResponse(true, targetUsername + "님께 초대를 보냈습니다.");
    }
    public static InviteUserResponse fail(String message) {
        return new InviteUserResponse(false, message);
    }

    public String getType()    { return type; }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public void setSuccess(boolean s) { this.success = s; }
    public void setMessage(String m)  { this.message = m; }
}

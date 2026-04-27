package com.dsc.sharededitor.dto.request;

public class InviteUserRequest {
    private String docId;
    private String targetUsername;

    public InviteUserRequest() {}
    public InviteUserRequest(String docId, String targetUsername) { this.docId = docId; this.targetUsername = targetUsername; }

    public String getDocId() { return docId; }
    public void setDocId(String docId) { this.docId = docId; }
    public String getTargetUsername() { return targetUsername; }
    public void setTargetUsername(String targetUsername) { this.targetUsername = targetUsername; }
}

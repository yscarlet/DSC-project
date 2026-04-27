package com.dsc.sharededitor.dto.request;

public class AcceptInviteRequest {
    private String docId;

    public AcceptInviteRequest() {}
    public AcceptInviteRequest(String docId) { this.docId = docId; }

    public String getDocId() { return docId; }
    public void setDocId(String docId) { this.docId = docId; }
}

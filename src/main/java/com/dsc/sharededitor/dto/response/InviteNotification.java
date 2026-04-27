package com.dsc.sharededitor.dto.response;

public class InviteNotification {
    private final String type = "INVITE_NOTIFICATION";
    private String fromUsername;
    private String docId;
    private String docName;

    public InviteNotification() {}
    public InviteNotification(String fromUsername, String docId, String docName) {
        this.fromUsername = fromUsername; this.docId = docId; this.docName = docName;
    }

    public String getType()         { return type; }
    public String getFromUsername() { return fromUsername; }
    public String getDocId()        { return docId; }
    public String getDocName()      { return docName; }
    public void setFromUsername(String f) { this.fromUsername = f; }
    public void setDocId(String d)        { this.docId = d; }
    public void setDocName(String n)      { this.docName = n; }
}

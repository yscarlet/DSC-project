package com.dsc.sharededitor.dto.response;

import com.dsc.sharededitor.domain.crdt.YItem;

import java.util.List;

public class AcceptInviteResponse {
    private final String type = "ACCEPT_INVITE_RESPONSE";
    private boolean success;
    private String docId;
    private String docName;
    private String message;
    private List<YItem> items;   // 현재 CRDT 상태 전달 (late-comer 동기화 - feature 3.2)

    public AcceptInviteResponse() {}
    private AcceptInviteResponse(boolean success, String docId, String docName, String message, List<YItem> items) {
        this.success = success; this.docId = docId; this.docName = docName;
        this.message = message; this.items = items;
    }

    public static AcceptInviteResponse success(String docId, String docName, List<YItem> items) {
        return new AcceptInviteResponse(true, docId, docName, "초대 수락 성공", items);
    }
    public static AcceptInviteResponse fail(String docId, String message) {
        return new AcceptInviteResponse(false, docId, null, message, null);
    }

    public String getType()      { return type; }
    public boolean isSuccess()   { return success; }
    public String getDocId()     { return docId; }
    public String getDocName()   { return docName; }
    public String getMessage()   { return message; }
    public List<YItem> getItems(){ return items; }
    public void setSuccess(boolean s)     { this.success = s; }
    public void setDocId(String d)        { this.docId = d; }
    public void setDocName(String n)      { this.docName = n; }
    public void setMessage(String m)      { this.message = m; }
    public void setItems(List<YItem> i)   { this.items = i; }
}

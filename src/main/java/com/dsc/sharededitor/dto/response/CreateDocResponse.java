package com.dsc.sharededitor.dto.response;

public class CreateDocResponse {
    private final String type = "CREATE_DOC_RESPONSE";
    private boolean success;
    private String docId;
    private String docName;
    private String message;

    public CreateDocResponse() {}
    private CreateDocResponse(boolean success, String docId, String docName, String message) {
        this.success = success; this.docId = docId; this.docName = docName; this.message = message;
    }

    public static CreateDocResponse success(String docId, String docName) {
        return new CreateDocResponse(true, docId, docName, "문서 생성 성공");
    }
    public static CreateDocResponse fail(String message) {
        return new CreateDocResponse(false, null, null, message);
    }

    public String getType()    { return type; }
    public boolean isSuccess() { return success; }
    public String getDocId()   { return docId; }
    public String getDocName() { return docName; }
    public String getMessage() { return message; }
    public void setSuccess(boolean s) { this.success = s; }
    public void setDocId(String d)    { this.docId = d; }
    public void setDocName(String n)  { this.docName = n; }
    public void setMessage(String m)  { this.message = m; }
}

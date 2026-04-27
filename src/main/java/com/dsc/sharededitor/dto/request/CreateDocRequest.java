package com.dsc.sharededitor.dto.request;

public class CreateDocRequest {
    private String docName;

    public CreateDocRequest() {}
    public CreateDocRequest(String docName) { this.docName = docName; }

    public String getDocName() { return docName; }
    public void setDocName(String docName) { this.docName = docName; }
}

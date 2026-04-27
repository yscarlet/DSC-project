package com.dsc.sharededitor.dto.request;

import com.dsc.sharededitor.domain.crdt.YItem;

import java.util.List;

public class TextInsertRequest {
    private String docId;
    private List<YItem> items;

    public TextInsertRequest() {}
    public TextInsertRequest(String docId, List<YItem> items) { this.docId = docId; this.items = items; }

    public String getDocId() { return docId; }
    public void setDocId(String docId) { this.docId = docId; }
    public List<YItem> getItems() { return items; }
    public void setItems(List<YItem> items) { this.items = items; }
}

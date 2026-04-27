package com.dsc.sharededitor.dto.request;

import com.dsc.sharededitor.domain.crdt.YItemId;

import java.util.List;

public class TextDeleteRequest {
    private String docId;
    private List<YItemId> itemIds;

    public TextDeleteRequest() {}
    public TextDeleteRequest(String docId, List<YItemId> itemIds) { this.docId = docId; this.itemIds = itemIds; }

    public String getDocId() { return docId; }
    public void setDocId(String docId) { this.docId = docId; }
    public List<YItemId> getItemIds() { return itemIds; }
    public void setItemIds(List<YItemId> itemIds) { this.itemIds = itemIds; }
}

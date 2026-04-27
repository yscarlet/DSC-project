package com.dsc.sharededitor.dto.response;

import com.dsc.sharededitor.domain.crdt.YItem;
import com.dsc.sharededitor.domain.crdt.YItemId;

import java.util.List;

/** 서버 → 문서 구독자 전체에게 브로드캐스트하는 편집 동기화 메시지 */
public class DocUpdateResponse {
    private final String type = "DOC_UPDATE";
    private String docId;
    private String operationType;        // "INSERT" | "DELETE"
    private String updatedBy;            // 클라이언트가 자신의 업데이트는 무시할 때 사용
    private List<YItem>   insertItems;
    private List<YItemId> deleteItemIds;

    public DocUpdateResponse() {}
    private DocUpdateResponse(String docId, String operationType, String updatedBy,
                              List<YItem> insertItems, List<YItemId> deleteItemIds) {
        this.docId = docId; this.operationType = operationType; this.updatedBy = updatedBy;
        this.insertItems = insertItems; this.deleteItemIds = deleteItemIds;
    }

    public static DocUpdateResponse insert(String docId, List<YItem> items, String updatedBy) {
        return new DocUpdateResponse(docId, "INSERT", updatedBy, items, null);
    }
    public static DocUpdateResponse delete(String docId, List<YItemId> itemIds, String updatedBy) {
        return new DocUpdateResponse(docId, "DELETE", updatedBy, null, itemIds);
    }

    public String getType()              { return type; }
    public String getDocId()             { return docId; }
    public String getOperationType()     { return operationType; }
    public String getUpdatedBy()         { return updatedBy; }
    public List<YItem> getInsertItems()  { return insertItems; }
    public List<YItemId> getDeleteItemIds() { return deleteItemIds; }
    public void setDocId(String d)             { this.docId = d; }
    public void setOperationType(String o)     { this.operationType = o; }
    public void setUpdatedBy(String u)         { this.updatedBy = u; }
    public void setInsertItems(List<YItem> i)  { this.insertItems = i; }
    public void setDeleteItemIds(List<YItemId> ids) { this.deleteItemIds = ids; }
}

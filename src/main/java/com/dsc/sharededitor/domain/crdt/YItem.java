package com.dsc.sharededitor.domain.crdt;

public class YItem {

    private YItemId id;
    private String content;
    private YItemId afterId;   // null = 문서 맨 앞에 삽입
    private boolean deleted;

    public YItem() {}

    public YItem(YItemId id, String content, YItemId afterId) {
        this.id = id;
        this.content = content;
        this.afterId = afterId;
        this.deleted = false;
    }

    public YItemId getId() { return id; }
    public void setId(YItemId id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public YItemId getAfterId() { return afterId; }
    public void setAfterId(YItemId afterId) { this.afterId = afterId; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}

package com.dsc.sharededitor.domain.crdt;

import java.util.*;

/**
 * Y.js YATA 알고리즘 기반 CRDT 텍스트.
 * 각 문자는 고유 YItemId를 가지며 afterId(왼쪽 이웃 ID)로 위치를 지정한다.
 * 같은 afterId를 가진 동시 삽입은 clientId → clock 순으로 결정론적 정렬.
 */
public class YText {

    private static final class Node {
        final YItem item;   // null = sentinel
        Node prev;
        Node next;
        Node(YItem item) { this.item = item; }
    }

    private final Node head = new Node(null);   // sentinel start
    private final Node tail = new Node(null);   // sentinel end
    private final Map<YItemId, Node> nodeMap = new HashMap<>();

    public YText() {
        head.next = tail;
        tail.prev = head;
    }

    // ── 삽입 ──────────────────────────────────────────────────────────────────

    public synchronized void integrate(YItem newItem) {
        Node afterNode = resolveNode(newItem.getAfterId());

        // afterId가 같은 항목들 사이에서 결정론적 위치 탐색
        Node insertAfterNode = afterNode;
        Node cur = afterNode.next;

        while (cur != tail) {
            if (!Objects.equals(cur.item.getAfterId(), newItem.getAfterId())) break;
            if (compareIds(cur.item.getId(), newItem.getId()) < 0) {
                insertAfterNode = cur;
                cur = cur.next;
            } else {
                break;
            }
        }

        Node newNode = new Node(newItem);
        Node next = insertAfterNode.next;
        newNode.prev = insertAfterNode;
        newNode.next = next;
        insertAfterNode.next = newNode;
        next.prev = newNode;
        nodeMap.put(newItem.getId(), newNode);
    }

    // ── 삭제 (tombstone) ─────────────────────────────────────────────────────

    public synchronized void delete(YItemId itemId) {
        Node node = nodeMap.get(itemId);
        if (node != null) node.item.setDeleted(true);
    }

    // ── 조회 ─────────────────────────────────────────────────────────────────

    public synchronized String getText() {
        StringBuilder sb = new StringBuilder();
        Node cur = head.next;
        while (cur != tail) {
            if (!cur.item.isDeleted()) sb.append(cur.item.getContent());
            cur = cur.next;
        }
        return sb.toString();
    }

    public synchronized int getVisibleLength() {
        int count = 0;
        Node cur = head.next;
        while (cur != tail) {
            if (!cur.item.isDeleted()) count++;
            cur = cur.next;
        }
        return count;
    }

    /** 보이는 텍스트 기준 0-based 인덱스의 아이템 ID 반환 */
    public synchronized YItemId getItemIdAt(int zeroBasedIndex) {
        int count = 0;
        Node cur = head.next;
        while (cur != tail) {
            if (!cur.item.isDeleted()) {
                if (count == zeroBasedIndex) return cur.item.getId();
                count++;
            }
            cur = cur.next;
        }
        return null;
    }

    /**
     * position번째 문자 뒤에 삽입할 때 사용할 afterId 반환.
     * position=0 → 맨 앞(afterId=null), position=n → n번째 보이는 문자 뒤
     */
    public synchronized YItemId getAfterIdForInsertAt(int position) {
        if (position <= 0) return null;
        int count = 0;
        YItemId last = null;
        Node cur = head.next;
        while (cur != tail) {
            if (!cur.item.isDeleted()) {
                count++;
                last = cur.item.getId();
                if (count == position) return last;
            }
            cur = cur.next;
        }
        return last;   // position >= 길이 → 맨 뒤에 삽입
    }

    /** 링크드리스트 순서대로 전체 YItem 목록 반환 (late-comer 동기화용) */
    public synchronized List<YItem> getAllItemsInOrder() {
        List<YItem> items = new ArrayList<>();
        Node cur = head.next;
        while (cur != tail) {
            items.add(cur.item);
            cur = cur.next;
        }
        return items;
    }

    /** 아이템 목록으로 YText 재구성 (late-comer 지원 - feature 3.2) */
    public static YText fromItems(List<YItem> items) {
        YText text = new YText();
        for (YItem item : items) {
            text.integrate(item);
        }
        return text;
    }

    // ── 내부 유틸 ─────────────────────────────────────────────────────────────

    private Node resolveNode(YItemId id) {
        if (id == null) return head;
        Node n = nodeMap.get(id);
        return n != null ? n : head;
    }

    private int compareIds(YItemId a, YItemId b) {
        int cmp = a.getClientId().compareTo(b.getClientId());
        return cmp != 0 ? cmp : Long.compare(a.getClock(), b.getClock());
    }
}

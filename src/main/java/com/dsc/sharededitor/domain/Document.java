package com.dsc.sharededitor.domain;

import com.dsc.sharededitor.domain.crdt.YText;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Document {

    private final String docId;
    private final String docName;
    private final String ownerUsername;
    private final YText yText = new YText();

    private final Set<String> memberUsernames       = ConcurrentHashMap.newKeySet();
    private final Set<String> activeSessionIds      = ConcurrentHashMap.newKeySet();
    private final Set<String> pendingInviteUsernames = ConcurrentHashMap.newKeySet();

    public Document(String docId, String docName, String ownerUsername) {
        this.docId = docId;
        this.docName = docName;
        this.ownerUsername = ownerUsername;
        memberUsernames.add(ownerUsername);
    }

    public String getDocId()         { return docId; }
    public String getDocName()       { return docName; }
    public String getOwnerUsername() { return ownerUsername; }
    public YText  getYText()         { return yText; }

    public boolean hasMember(String username)       { return memberUsernames.contains(username); }
    public boolean hasPendingInvite(String username) { return pendingInviteUsernames.contains(username); }

    public void addMember(String username) {
        memberUsernames.add(username);
        pendingInviteUsernames.remove(username);
    }

    public void addPendingInvite(String username)   { pendingInviteUsernames.add(username); }
    public void addActiveSession(String sessionId)  { activeSessionIds.add(sessionId); }
    public void removeActiveSession(String sessionId) { activeSessionIds.remove(sessionId); }

    public Set<String> getActiveSessionIds() {
        return Collections.unmodifiableSet(activeSessionIds);
    }
}

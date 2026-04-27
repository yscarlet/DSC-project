package com.dsc.sharededitor.service;

import com.dsc.sharededitor.component.SessionRegistry;
import com.dsc.sharededitor.domain.Document;
import com.dsc.sharededitor.domain.crdt.YItem;
import com.dsc.sharededitor.domain.crdt.YItemId;
import com.dsc.sharededitor.domain.crdt.YText;
import com.dsc.sharededitor.dto.request.*;
import com.dsc.sharededitor.dto.response.*;
import com.dsc.sharededitor.repository.DocumentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final SessionRegistry sessionRegistry;

    public DocumentService(DocumentRepository documentRepository, SessionRegistry sessionRegistry) {
        this.documentRepository = documentRepository;
        this.sessionRegistry = sessionRegistry;
    }

    // ── 문서 생성 ─────────────────────────────────────────────────────────────

    public CreateDocResponse createDoc(String sessionId, String username, CreateDocRequest request) {
        String docId = UUID.randomUUID().toString();
        Document doc = new Document(docId, request.getDocName(), username);
        doc.addActiveSession(sessionId);
        documentRepository.save(doc);
        return CreateDocResponse.success(docId, request.getDocName());
    }

    // ── 사용자 초대 ───────────────────────────────────────────────────────────

    public InviteUserResponse inviteUser(String username, InviteUserRequest request) {
        Document doc = documentRepository.findById(request.getDocId()).orElse(null);
        if (doc == null)
            return InviteUserResponse.fail("문서를 찾을 수 없습니다.");
        if (!doc.hasMember(username))
            return InviteUserResponse.fail("해당 문서에 대한 권한이 없습니다.");
        if (!sessionRegistry.isOnline(request.getTargetUsername()))
            return InviteUserResponse.fail(request.getTargetUsername() + "님이 온라인 상태가 아닙니다.");

        doc.addPendingInvite(request.getTargetUsername());
        return InviteUserResponse.success(request.getTargetUsername());
    }

    // ── 초대 수락 (late-comer 동기화 포함 - feature 3.2) ─────────────────────

    public AcceptInviteResponse acceptInvite(String sessionId, String username, AcceptInviteRequest request) {
        Document doc = documentRepository.findById(request.getDocId()).orElse(null);
        if (doc == null)
            return AcceptInviteResponse.fail(request.getDocId(), "문서를 찾을 수 없습니다.");
        if (!doc.hasPendingInvite(username))
            return AcceptInviteResponse.fail(request.getDocId(), "유효한 초대가 없습니다.");

        doc.addMember(username);
        doc.addActiveSession(sessionId);

        List<YItem> items = doc.getYText().getAllItemsInOrder();
        return AcceptInviteResponse.success(doc.getDocId(), doc.getDocName(), items);
    }

    // ── 텍스트 삽입 ───────────────────────────────────────────────────────────

    public DocUpdateResponse insertText(String username, TextInsertRequest request) {
        Document doc = documentRepository.findById(request.getDocId()).orElse(null);
        if (doc == null || !doc.hasMember(username)) return null;

        YText yText = doc.getYText();
        for (YItem item : request.getItems()) {
            yText.integrate(item);
        }
        System.out.println("[DocumentService] 삽입 후: \"" + yText.getText() + "\"");
        return DocUpdateResponse.insert(request.getDocId(), request.getItems(), username);
    }

    // ── 텍스트 삭제 ───────────────────────────────────────────────────────────

    public DocUpdateResponse deleteText(String username, TextDeleteRequest request) {
        Document doc = documentRepository.findById(request.getDocId()).orElse(null);
        if (doc == null || !doc.hasMember(username)) return null;

        YText yText = doc.getYText();
        for (YItemId itemId : request.getItemIds()) {
            yText.delete(itemId);
        }
        System.out.println("[DocumentService] 삭제 후: \"" + yText.getText() + "\"");
        return DocUpdateResponse.delete(request.getDocId(), request.getItemIds(), username);
    }

    // ── 연결 해제 ─────────────────────────────────────────────────────────────

    public void handleDisconnect(String sessionId) {
        documentRepository.findAll().forEach(doc -> doc.removeActiveSession(sessionId));
    }

    public String getDocName(String docId) {
        return documentRepository.findById(docId).map(Document::getDocName).orElse(null);
    }
}

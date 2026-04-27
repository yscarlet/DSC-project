package com.dsc.sharededitor.controller;

import com.dsc.sharededitor.component.SessionRegistry;
import com.dsc.sharededitor.dto.request.*;
import com.dsc.sharededitor.dto.response.*;
import com.dsc.sharededitor.service.DocumentService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class DocumentController {

    private final DocumentService documentService;
    private final SessionRegistry sessionRegistry;
    private final SimpMessagingTemplate messagingTemplate;

    public DocumentController(DocumentService documentService,
                              SessionRegistry sessionRegistry,
                              SimpMessagingTemplate messagingTemplate) {
        this.documentService = documentService;
        this.sessionRegistry = sessionRegistry;
        this.messagingTemplate = messagingTemplate;
    }

    // ── 문서 생성 ──────────────────────────────────────────────────────────────
    // 클라이언트 → /app/doc/create
    // 응답     → /topic/user/{username}
    @MessageMapping("/doc/create")
    public void createDoc(@Payload CreateDocRequest request,
                          SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String username = sessionRegistry.getUsername(sessionId);
        if (username == null) return;

        CreateDocResponse response = documentService.createDoc(sessionId, username, request);
        messagingTemplate.convertAndSend("/topic/user/" + username, response);
    }

    // ── 사용자 초대 ────────────────────────────────────────────────────────────
    // 클라이언트 → /app/doc/invite
    // 응답     → /topic/user/{username}        (초대자)
    // 알림     → /topic/user/{targetUsername}  (피초대자)
    @MessageMapping("/doc/invite")
    public void inviteUser(@Payload InviteUserRequest request,
                           SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String username = sessionRegistry.getUsername(sessionId);
        if (username == null) return;

        InviteUserResponse response = documentService.inviteUser(username, request);
        messagingTemplate.convertAndSend("/topic/user/" + username, response);

        if (response.isSuccess()) {
            String docName = documentService.getDocName(request.getDocId());
            messagingTemplate.convertAndSend("/topic/user/" + request.getTargetUsername(),
                    new InviteNotification(username, request.getDocId(), docName));
        }
    }

    // ── 초대 수락 ──────────────────────────────────────────────────────────────
    // 클라이언트 → /app/doc/accept
    // 응답     → /topic/user/{username}  (현재 CRDT 상태 포함 - late-comer)
    @MessageMapping("/doc/accept")
    public void acceptInvite(@Payload AcceptInviteRequest request,
                             SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String username = sessionRegistry.getUsername(sessionId);
        if (username == null) return;

        AcceptInviteResponse response = documentService.acceptInvite(sessionId, username, request);
        messagingTemplate.convertAndSend("/topic/user/" + username, response);
    }

    // ── 텍스트 삽입 ────────────────────────────────────────────────────────────
    // 클라이언트 → /app/doc/insert
    // 브로드캐스트 → /topic/doc/{docId}  (문서 구독자 전체)
    @MessageMapping("/doc/insert")
    public void insertText(@Payload TextInsertRequest request,
                           SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String username = sessionRegistry.getUsername(sessionId);
        if (username == null) return;

        DocUpdateResponse update = documentService.insertText(username, request);
        if (update != null) {
            messagingTemplate.convertAndSend("/topic/doc/" + request.getDocId(), update);
        }
    }

    // ── 텍스트 삭제 ────────────────────────────────────────────────────────────
    // 클라이언트 → /app/doc/delete
    // 브로드캐스트 → /topic/doc/{docId}
    @MessageMapping("/doc/delete")
    public void deleteText(@Payload TextDeleteRequest request,
                           SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String username = sessionRegistry.getUsername(sessionId);
        if (username == null) return;

        DocUpdateResponse update = documentService.deleteText(username, request);
        if (update != null) {
            messagingTemplate.convertAndSend("/topic/doc/" + request.getDocId(), update);
        }
    }
}

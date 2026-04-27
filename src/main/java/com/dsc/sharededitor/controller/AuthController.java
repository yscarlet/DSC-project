package com.dsc.sharededitor.controller;

import com.dsc.sharededitor.dto.request.LoginRequest;
import com.dsc.sharededitor.dto.response.LoginResponse;
import com.dsc.sharededitor.dto.response.UserStatusNotification;
import com.dsc.sharededitor.service.AuthService;
import com.dsc.sharededitor.service.DocumentService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Controller
public class AuthController {

    private final AuthService authService;
    private final DocumentService documentService;
    private final SimpMessagingTemplate messagingTemplate;

    public AuthController(AuthService authService,
                          DocumentService documentService,
                          SimpMessagingTemplate messagingTemplate) {
        this.authService = authService;
        this.documentService = documentService;
        this.messagingTemplate = messagingTemplate;
    }

    // ── 로그인 ────────────────────────────────────────────────────────────────
    // 클라이언트 → /app/auth/login
    // 응답     → /topic/user/{username}  (개인)
    // 성공 시  → /topic/global           (전체 알림)
    @MessageMapping("/auth/login")
    public void login(@Payload LoginRequest request,
                      SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        LoginResponse response = authService.login(sessionId, request);

        messagingTemplate.convertAndSend("/topic/user/" + request.getUsername(), response);

        if (response.isSuccess()) {
            messagingTemplate.convertAndSend("/topic/global",
                    new UserStatusNotification(request.getUsername(), "JOINED",
                            request.getUsername() + "님이 접속했습니다."));
        }
    }

    // ── 로그아웃 ──────────────────────────────────────────────────────────────
    // 클라이언트 → /app/auth/logout
    @MessageMapping("/auth/logout")
    public void logout(SimpMessageHeaderAccessor headerAccessor) {
        handleLeave(headerAccessor.getSessionId(), "로그아웃");
    }

    // ── WebSocket 연결 해제 이벤트 ────────────────────────────────────────────
    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        handleLeave(event.getSessionId(), "연결 끊김");
    }

    private void handleLeave(String sessionId, String reason) {
        String username = authService.logout(sessionId);
        documentService.handleDisconnect(sessionId);

        if (username != null) {
            messagingTemplate.convertAndSend("/topic/global",
                    new UserStatusNotification(username, "LEFT",
                            username + "님이 접속 해제했습니다. (" + reason + ")"));
        }
    }
}

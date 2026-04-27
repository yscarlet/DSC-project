package com.dsc.sharededitor.component;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 세션 ID ↔ 사용자명 양방향 매핑 관리.
 * Spring이 관리하는 싱글톤 빈이므로 모든 컨트롤러/서비스에서 공유된다.
 */
@Component
public class SessionRegistry {

    private final Map<String, String> sessionToUsername = new ConcurrentHashMap<>();
    private final Map<String, String> usernameToSession = new ConcurrentHashMap<>();

    public void register(String sessionId, String username) {
        sessionToUsername.put(sessionId, username);
        usernameToSession.put(username, sessionId);
    }

    /** 세션 제거 후 매핑된 사용자명 반환 (없으면 null) */
    public String removeBySessionId(String sessionId) {
        String username = sessionToUsername.remove(sessionId);
        if (username != null) usernameToSession.remove(username);
        return username;
    }

    public String getUsername(String sessionId) {
        return sessionToUsername.get(sessionId);
    }

    public String getSessionId(String username) {
        return usernameToSession.get(username);
    }

    public boolean isOnline(String username) {
        return usernameToSession.containsKey(username);
    }
}

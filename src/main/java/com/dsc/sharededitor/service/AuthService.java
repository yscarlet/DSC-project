package com.dsc.sharededitor.service;

import com.dsc.sharededitor.component.SessionRegistry;
import com.dsc.sharededitor.dto.request.LoginRequest;
import com.dsc.sharededitor.dto.response.LoginResponse;
import com.dsc.sharededitor.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final SessionRegistry sessionRegistry;

    public AuthService(UserRepository userRepository, SessionRegistry sessionRegistry) {
        this.userRepository = userRepository;
        this.sessionRegistry = sessionRegistry;
    }

    public LoginResponse login(String sessionId, LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        if (!userRepository.matches(username, password))
            return LoginResponse.fail("아이디 또는 비밀번호가 올바르지 않습니다.");

        if (sessionRegistry.isOnline(username))
            return LoginResponse.fail("이미 접속 중인 계정입니다.");

        sessionRegistry.register(sessionId, username);
        return LoginResponse.success(username);
    }

    /** 로그아웃 처리 후 사용자명 반환 (없으면 null) */
    public String logout(String sessionId) {
        return sessionRegistry.removeBySessionId(sessionId);
    }
}

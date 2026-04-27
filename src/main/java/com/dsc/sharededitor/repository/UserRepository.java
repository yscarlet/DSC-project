package com.dsc.sharededitor.repository;

import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public class UserRepository {

    // 추후 JPA Entity로 교체 가능한 인메모리 구현
    private final Map<String, String> store = Map.of(
            "user1", "1234",
            "user2", "2345",
            "user3", "3456",
            "user4", "4567"
    );

    public boolean matches(String username, String password) {
        return password.equals(store.get(username));
    }
}

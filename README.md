# DscSharedEditor

실시간 협업 문서 편집기 — CRDT(Y.js YATA 알고리즘) 기반 동시 편집 충돌 해결

## 기술 스택

- **언어**: Java 21
- **프레임워크**: Spring Boot 4.0.3
- **통신**: WebSocket + STOMP
- **동기화 알고리즘**: CRDT (Y.js YATA)

## 프로젝트 구조

```
src/main/java/com/dsc/sharededitor/
├── config/         # WebSocket 설정
├── controller/     # STOMP 메시지 라우팅
├── service/        # 비즈니스 로직
├── repository/     # 인메모리 저장소
├── component/      # 세션 관리
├── domain/
│   ├── crdt/       # YItem, YItemId, YText (CRDT 핵심)
│   └── Document.java
├── dto/
│   ├── request/
│   └── response/
└── client/         # 콘솔 클라이언트
```

## 실행 방법

### 요구사항

- Java 21

### 1. 서버 실행

```bash
./gradlew bootRun
```

서버가 `localhost:8080`에서 실행됩니다.

### 2. 클라이언트 실행

IntelliJ에서 `ConsoleClientRunner`의 `main()` 메서드를 직접 실행합니다.
여러 유저를 테스트하려면 실행 구성(Run Configuration)을 여러 개 만들어 동시에 실행하세요.

## 테스트 계정

| 아이디 | 비밀번호 |
|--------|----------|
| user1  | 1234     |
| user2  | 2345     |
| user3  | 3456     |
| user4  | 4567     |

## 주요 기능

| 메뉴 | 기능 |
|------|------|
| 1    | 로그인 |
| 2    | 로그아웃 |
| 3    | 문서 생성 |
| 4    | 사용자 초대 |
| 5    | 초대 수락 |
| 6    | 문서 선택 |
| 7    | 텍스트 삽입 |
| 8    | 텍스트 삭제 |
| 9    | 텍스트 수정 |

## 동작 원리

### STOMP 토픽 구조

| 토픽 | 용도 |
|------|------|
| `/topic/global` | 전체 접속/해제 알림 |
| `/topic/user/{username}` | 개인 응답 |
| `/topic/doc/{docId}` | 문서 실시간 업데이트 브로드캐스트 |

### CRDT 동시 편집 충돌 해결

각 글자는 고유 ID `(clientId, clock)`를 가진 `YItem`으로 표현됩니다.
두 클라이언트가 동시에 같은 위치에 삽입할 경우 `clientId` 사전순으로 순서를 결정해 모든 클라이언트에서 동일한 결과를 보장합니다.
삭제는 실제 제거 대신 tombstone(논리적 삭제) 방식을 사용합니다.

### Late-comer 동기화

초대를 수락한 클라이언트는 서버로부터 문서의 전체 CRDT 상태(`List<YItem>`)를 받아 로컬 `YText`를 재구성합니다.

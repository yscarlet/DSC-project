package com.dsc.sharededitor.client;

import com.dsc.sharededitor.domain.crdt.YItem;
import com.dsc.sharededitor.domain.crdt.YItemId;
import com.dsc.sharededitor.domain.crdt.YText;
import com.dsc.sharededitor.dto.request.*;
import com.dsc.sharededitor.dto.response.*;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StompConsoleClient {

    private StompSession session;
    private String username;
    private String currentDocId;
    private long nextClock = 1;

    private final Map<String, YText>  localDocs     = new ConcurrentHashMap<>();
    private final Map<String, String> localDocNames = new ConcurrentHashMap<>();
    private final Map<String, String> pendingInvites = new ConcurrentHashMap<>(); // docId → docName

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── 시작 ──────────────────────────────────────────────────────────────────

    public void start() throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new JsonStringConverter());

        session = stompClient.connectAsync("ws://localhost:8080/ws",
                new StompSessionHandlerAdapter() {
                    @Override
                    public void afterConnected(StompSession s, StompHeaders h) {
                        System.out.println("[Client] 서버에 연결되었습니다.");
                    }
                    @Override
                    public void handleTransportError(StompSession s, Throwable ex) {
                        System.out.println("[Client] 연결 오류: " + ex.getMessage());
                    }
                }).get(10, java.util.concurrent.TimeUnit.SECONDS);

        // 전체 알림 구독 (접속/해제 알림)
        session.subscribe("/topic/global", new RawFrameHandler());

        Scanner scanner = new Scanner(System.in);
        runMenu(scanner);
    }

    // ── 메뉴 루프 ─────────────────────────────────────────────────────────────

    private void runMenu(Scanner scanner) {
        while (true) {
            printMenu();
            String cmd = scanner.nextLine().trim();
            try {
                switch (cmd) {
                    case "1" -> login(scanner);
                    case "2" -> logout();
                    case "3" -> createDoc(scanner);
                    case "4" -> inviteUser(scanner);
                    case "5" -> acceptInvite(scanner);
                    case "6" -> selectDoc(scanner);
                    case "7" -> insertText(scanner);
                    case "8" -> deleteText(scanner);
                    case "9" -> modifyText(scanner);
                    case "0" -> { logout(); System.out.println("[Client] 종료합니다."); return; }
                    default  -> System.out.println("[Client] 올바른 번호를 입력하세요.");
                }
            } catch (Exception e) {
                System.out.println("[Client] 오류: " + e.getMessage());
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("=== 메뉴 === "
                + (username != null ? "[" + username + "]" : "")
                + (currentDocId != null ? " 문서:" + currentDocId.substring(0, 8) + "..." : ""));
        System.out.println("1. 로그인       2. 로그아웃");
        System.out.println("3. 문서 생성    4. 사용자 초대    5. 초대 수락");
        System.out.println("6. 문서 선택    7. 텍스트 삽입    8. 텍스트 삭제    9. 텍스트 수정");
        System.out.println("0. 종료");
        System.out.print("선택: ");
    }

    // ── 인증 ─────────────────────────────────────────────────────────────────

    private void login(Scanner scanner) {
        System.out.print("아이디: ");    String uname = scanner.nextLine().trim();
        System.out.print("비밀번호: "); String pwd   = scanner.nextLine().trim();

        // 개인 응답 토픽 구독 (로그인 요청 전에 먼저)
        session.subscribe("/topic/user/" + uname, new RawFrameHandler());

        username = uname;
        send("/app/auth/login", new LoginRequest(uname, pwd));
    }

    private void logout() {
        if (username == null) return;
        send("/app/auth/logout", Map.of());
        username = null;
        currentDocId = null;
    }

    // ── 문서 관리 ─────────────────────────────────────────────────────────────

    private void createDoc(Scanner scanner) {
        if (!checkLogin()) return;
        System.out.print("문서 이름: ");
        send("/app/doc/create", new CreateDocRequest(scanner.nextLine().trim()));
    }

    private void inviteUser(Scanner scanner) {
        if (!checkLogin() || !checkDoc()) return;
        System.out.print("초대할 사용자 아이디: ");
        send("/app/doc/invite", new InviteUserRequest(currentDocId, scanner.nextLine().trim()));
    }

    private void acceptInvite(Scanner scanner) {
        if (!checkLogin()) return;
        if (pendingInvites.isEmpty()) { System.out.println("[Client] 대기 중인 초대가 없습니다."); return; }

        System.out.println("[대기 중인 초대]");
        pendingInvites.forEach((id, name) ->
                System.out.println("  " + id.substring(0, 8) + "... → " + name));
        System.out.print("수락할 문서 ID (앞 8자 이상): ");
        String input = scanner.nextLine().trim();
        String matched = pendingInvites.keySet().stream().filter(id -> id.startsWith(input)).findFirst().orElse(null);
        if (matched == null) { System.out.println("[Client] 해당 초대를 찾을 수 없습니다."); return; }

        // 문서 업데이트 토픽 구독
        session.subscribe("/topic/doc/" + matched, new RawFrameHandler());
        send("/app/doc/accept", new AcceptInviteRequest(matched));
        pendingInvites.remove(matched);
    }

    private void selectDoc(Scanner scanner) {
        if (localDocs.isEmpty()) { System.out.println("[Client] 보유 문서가 없습니다."); return; }
        System.out.println("[보유 문서 목록]");
        localDocs.forEach((id, yt) ->
                System.out.println("  " + id.substring(0, 8) + "... [" + localDocNames.getOrDefault(id, "?") + "] → \"" + yt.getText() + "\""));
        System.out.print("선택 (앞 8자 이상): ");
        String input = scanner.nextLine().trim();
        String matched = localDocs.keySet().stream().filter(id -> id.startsWith(input)).findFirst().orElse(null);
        if (matched == null) { System.out.println("[Client] 문서를 찾을 수 없습니다."); return; }
        currentDocId = matched;
        System.out.println("[Client] 활성 문서: " + localDocNames.getOrDefault(matched, matched));
        System.out.println("[현재 내용] \"" + localDocs.get(matched).getText() + "\"");
    }

    // ── 텍스트 편집 ───────────────────────────────────────────────────────────

    private void insertText(Scanner scanner) {
        YText yText = getActiveDoc();
        if (yText == null) return;

        int len = yText.getVisibleLength();
        System.out.println("[현재 문서] \"" + yText.getText() + "\" (길이: " + len + ")");
        System.out.println("삽입 위치 (0=맨 앞, " + len + "=맨 뒤): ");
        System.out.print("위치: ");
        int pos = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("삽입할 텍스트: ");
        String text = scanner.nextLine();
        if (text.isEmpty()) return;

        List<YItem> items = buildInsertItems(yText, pos, text);
        for (YItem item : items) yText.integrate(item);  // 낙관적 로컬 적용

        send("/app/doc/insert", new TextInsertRequest(currentDocId, items));
        System.out.println("[현재 문서] \"" + yText.getText() + "\"");
    }

    private void deleteText(Scanner scanner) {
        YText yText = getActiveDoc();
        if (yText == null) return;
        int len = yText.getVisibleLength();
        if (len == 0) { System.out.println("[Client] 문서가 비어 있습니다."); return; }

        System.out.println("[현재 문서] \"" + yText.getText() + "\" (길이: " + len + ")");
        System.out.print("삭제 시작 위치 (1~" + len + "): ");
        int startPos = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("삭제할 개수: ");
        int count = Integer.parseInt(scanner.nextLine().trim());

        List<YItemId> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            YItemId id = yText.getItemIdAt(startPos - 1 + i);
            if (id != null) ids.add(id);
        }
        for (YItemId id : ids) yText.delete(id);

        send("/app/doc/delete", new TextDeleteRequest(currentDocId, ids));
        System.out.println("[현재 문서] \"" + yText.getText() + "\"");
    }

    private void modifyText(Scanner scanner) {
        YText yText = getActiveDoc();
        if (yText == null) return;
        int len = yText.getVisibleLength();
        if (len == 0) { System.out.println("[Client] 문서가 비어 있습니다."); return; }

        System.out.println("[현재 문서] \"" + yText.getText() + "\" (길이: " + len + ")");
        System.out.print("수정 시작 위치 (1~" + len + "): ");
        int startPos = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("수정할 문자 수: ");
        int count = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("새로운 텍스트: ");
        String newText = scanner.nextLine();

        // 삭제할 ID 미리 수집
        List<YItemId> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            YItemId id = yText.getItemIdAt(startPos - 1 + i);
            if (id != null) ids.add(id);
        }
        // 삽입 위치 결정 (삭제 전)
        YItemId afterId = yText.getAfterIdForInsertAt(startPos - 1);

        // 삭제
        for (YItemId id : ids) yText.delete(id);
        send("/app/doc/delete", new TextDeleteRequest(currentDocId, ids));

        // 삽입
        List<YItem> insertItems = new ArrayList<>();
        for (int i = 0; i < newText.length(); i++) {
            YItemId newId = new YItemId(username, nextClock++);
            YItem item = new YItem(newId, String.valueOf(newText.charAt(i)), afterId);
            insertItems.add(item);
            afterId = newId;
        }
        for (YItem item : insertItems) yText.integrate(item);
        send("/app/doc/insert", new TextInsertRequest(currentDocId, insertItems));

        System.out.println("[현재 문서] \"" + yText.getText() + "\"");
    }

    // ── 서버 메시지 수신 처리 ─────────────────────────────────────────────────

    private void handleServerMessage(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            String type = root.path("type").asText("");
            System.out.println();
            switch (type) {
                case "LOGIN_RESPONSE"         -> handleLoginResponse(json);
                case "USER_STATUS"            -> handleUserStatus(json);
                case "CREATE_DOC_RESPONSE"    -> handleCreateDocResponse(json);
                case "INVITE_USER_RESPONSE"   -> handleInviteUserResponse(json);
                case "INVITE_NOTIFICATION"    -> handleInviteNotification(json);
                case "ACCEPT_INVITE_RESPONSE" -> handleAcceptInviteResponse(json);
                case "DOC_UPDATE"             -> handleDocUpdate(json);
                default -> System.out.println("[서버] " + json);
            }
        } catch (Exception e) {
            System.out.println("[서버] " + json);
        }
        System.out.print("선택: ");
    }

    private void handleLoginResponse(String json) throws Exception {
        LoginResponse r = objectMapper.readValue(json, LoginResponse.class);
        System.out.println("[로그인] " + (r.isSuccess() ? "성공" : "실패: " + r.getMessage()));
        if (!r.isSuccess()) username = null;
    }

    private void handleUserStatus(String json) throws Exception {
        UserStatusNotification n = objectMapper.readValue(json, UserStatusNotification.class);
        System.out.println("[알림] " + n.getMessage());
    }

    private void handleCreateDocResponse(String json) throws Exception {
        CreateDocResponse r = objectMapper.readValue(json, CreateDocResponse.class);
        if (r.isSuccess()) {
            localDocs.put(r.getDocId(), new YText());
            localDocNames.put(r.getDocId(), r.getDocName());
            currentDocId = r.getDocId();
            // 문서 업데이트 토픽 구독
            session.subscribe("/topic/doc/" + r.getDocId(), new RawFrameHandler());
            System.out.println("[문서 생성] '" + r.getDocName() + "' (" + r.getDocId().substring(0, 8) + "...)");
        } else {
            System.out.println("[문서 생성 실패] " + r.getMessage());
        }
    }

    private void handleInviteUserResponse(String json) throws Exception {
        InviteUserResponse r = objectMapper.readValue(json, InviteUserResponse.class);
        System.out.println("[초대] " + (r.isSuccess() ? "성공" : "실패") + " - " + r.getMessage());
    }

    private void handleInviteNotification(String json) throws Exception {
        InviteNotification n = objectMapper.readValue(json, InviteNotification.class);
        pendingInvites.put(n.getDocId(), n.getDocName());
        System.out.println("[초대 알림] " + n.getFromUsername() + "님이 '" + n.getDocName() + "' 문서에 초대했습니다.");
        System.out.println("  → 메뉴 5번으로 수락하세요.");
    }

    private void handleAcceptInviteResponse(String json) throws Exception {
        AcceptInviteResponse r = objectMapper.readValue(json, AcceptInviteResponse.class);
        if (r.isSuccess()) {
            YText yText = YText.fromItems(r.getItems()); // late-comer 동기화
            localDocs.put(r.getDocId(), yText);
            localDocNames.put(r.getDocId(), r.getDocName());
            currentDocId = r.getDocId();
            System.out.println("[초대 수락] '" + r.getDocName() + "' (" + r.getDocId().substring(0, 8) + "...)");
            System.out.println("[현재 내용] \"" + yText.getText() + "\"");
        } else {
            System.out.println("[초대 수락 실패] " + r.getMessage());
        }
    }

    private void handleDocUpdate(String json) throws Exception {
        DocUpdateResponse u = objectMapper.readValue(json, DocUpdateResponse.class);
        YText yText = localDocs.get(u.getDocId());
        if (yText == null) return;

        // 자신의 업데이트는 이미 로컬에 적용했으므로 무시
        if (username != null && username.equals(u.getUpdatedBy())) return;

        if ("INSERT".equals(u.getOperationType()) && u.getInsertItems() != null)
            u.getInsertItems().forEach(yText::integrate);
        else if ("DELETE".equals(u.getOperationType()) && u.getDeleteItemIds() != null)
            u.getDeleteItemIds().forEach(yText::delete);

        String name = localDocNames.getOrDefault(u.getDocId(), u.getDocId().substring(0, 8));
        System.out.println("[실시간 업데이트] '" + name + "' → \"" + yText.getText() + "\"");
    }

    // ── 내부 유틸 ─────────────────────────────────────────────────────────────

    private List<YItem> buildInsertItems(YText yText, int position, String text) {
        List<YItem> items = new ArrayList<>();
        YItemId afterId = yText.getAfterIdForInsertAt(position);
        for (int i = 0; i < text.length(); i++) {
            YItemId newId = new YItemId(username, nextClock++);
            items.add(new YItem(newId, String.valueOf(text.charAt(i)), afterId));
            afterId = newId;
        }
        return items;
    }

    private void send(String destination, Object payload) {
        try {
            StompHeaders headers = new StompHeaders();
            headers.setDestination(destination);
            headers.setContentType(org.springframework.util.MimeTypeUtils.APPLICATION_JSON);
            String json = objectMapper.writeValueAsString(payload);
            session.send(headers, json);
        } catch (JacksonException e) {
            System.out.println("[Client] 전송 오류: " + e.getMessage());
        }
    }

    private YText getActiveDoc() {
        if (!checkLogin() || !checkDoc()) return null;
        return localDocs.get(currentDocId);
    }

    private boolean checkLogin() {
        if (username == null) { System.out.println("[Client] 먼저 로그인하세요."); return false; }
        return true;
    }

    private boolean checkDoc() {
        if (currentDocId == null) { System.out.println("[Client] 먼저 문서를 선택하세요."); return false; }
        return true;
    }

    // ── 메시지 변환기 (application/json → String) ────────────────────────────

    private static class JsonStringConverter extends AbstractMessageConverter {
        JsonStringConverter() {
            super(MimeTypeUtils.APPLICATION_JSON, MimeTypeUtils.TEXT_PLAIN, MimeType.valueOf("*/*"));
        }

        @Override
        protected boolean supports(Class<?> clazz) { return String.class == clazz; }

        @Override
        protected Object convertFromInternal(Message<?> message, Class<?> targetClass, Object hint) {
            Object payload = message.getPayload();
            if (payload instanceof byte[] b) return new String(b, java.nio.charset.StandardCharsets.UTF_8);
            return payload.toString();
        }

        @Override
        protected Object convertToInternal(Object payload, MessageHeaders headers, Object hint) {
            return payload.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    // ── STOMP 프레임 핸들러 ───────────────────────────────────────────────────

    private class RawFrameHandler implements StompFrameHandler {
        @Override
        public Type getPayloadType(StompHeaders headers) { return String.class; }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            handleServerMessage((String) payload);
        }
    }
}

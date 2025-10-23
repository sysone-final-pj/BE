# WebSocket 알림 연동 가이드

## 개요
Monito 플랫폼의 실시간 알림 시스템은 WebSocket을 사용하여 사용자에게 즉시 알림을 전달합니다.

## WebSocket 엔드포인트

```
ws://localhost:8080/ws/alerts?userId={userId}
```

- **프로토콜**: WebSocket (ws://)
- **엔드포인트**: `/ws/alerts`
- **필수 파라미터**: `userId` (사용자 ID)

### 프로덕션 환경
```
wss://your-domain.com/ws/alerts?userId={userId}
```
- HTTPS 환경에서는 `wss://` 프로토콜 사용

## 연결 방법

### 1. JavaScript (Vanilla)

```javascript
// WebSocket 연결
const userId = 1; // 로그인한 사용자 ID
const ws = new WebSocket(`ws://localhost:8080/ws/alerts?userId=${userId}`);

// 연결 성공
ws.onopen = function() {
    console.log('WebSocket 연결 성공');
};

// 메시지 수신
ws.onmessage = function(event) {
    const data = JSON.parse(event.data);
    console.log('알림 수신:', data);

    // 알림 타입별 처리
    switch(data.type) {
        case 'CRITICAL':
            showCriticalAlert(data);
            break;
        case 'WARNING':
            showWarningAlert(data);
            break;
        case 'INFO':
            showInfoAlert(data);
            break;
        case 'ALERT_READ_STATUS':
            updateAlertReadStatus(data);
            break;
        case 'ALERT_DELETED':
            removeAlertFromUI(data);
            break;
    }
};

// 에러 처리
ws.onerror = function(error) {
    console.error('WebSocket 에러:', error);
};

// 연결 종료
ws.onclose = function() {
    console.log('WebSocket 연결 종료');
    // 재연결 로직 구현 권장
};

// 페이지 언로드 시 연결 해제
window.addEventListener('beforeunload', function() {
    ws.close();
});
```

### 2. React 예제

```jsx
import { useEffect, useState, useRef } from 'react';

function useWebSocket(userId) {
    const [notifications, setNotifications] = useState([]);
    const wsRef = useRef(null);

    useEffect(() => {
        // WebSocket 연결
        const ws = new WebSocket(`ws://localhost:8080/ws/alerts?userId=${userId}`);
        wsRef.current = ws;

        ws.onopen = () => {
            console.log('WebSocket 연결 성공');
        };

        ws.onmessage = (event) => {
            const data = JSON.parse(event.data);
            console.log('알림 수신:', data);

            // 새 알림 추가
            if (data.type !== 'ALERT_READ_STATUS' && data.type !== 'ALERT_DELETED') {
                setNotifications(prev => [data, ...prev]);
            }

            // 읽음 처리 업데이트
            if (data.type === 'ALERT_READ_STATUS') {
                setNotifications(prev =>
                    prev.map(n => n.data === data.data ? { ...n, isRead: true } : n)
                );
            }

            // 삭제 처리
            if (data.type === 'ALERT_DELETED') {
                setNotifications(prev =>
                    prev.filter(n => n.data !== data.data)
                );
            }
        };

        ws.onerror = (error) => {
            console.error('WebSocket 에러:', error);
        };

        ws.onclose = () => {
            console.log('WebSocket 연결 종료');
        };

        // 클린업
        return () => {
            ws.close();
        };
    }, [userId]);

    return { notifications, ws: wsRef.current };
}

// 컴포넌트에서 사용
function NotificationComponent() {
    const userId = 1; // 실제로는 로그인 정보에서 가져옴
    const { notifications } = useWebSocket(userId);

    return (
        <div>
            <h2>알림 ({notifications.length})</h2>
            {notifications.map((notif, idx) => (
                <div key={idx} className={`alert alert-${notif.type.toLowerCase()}`}>
                    <strong>{notif.title}</strong>
                    <p>{notif.message}</p>
                    <small>{new Date(notif.timestamp).toLocaleString()}</small>
                </div>
            ))}
        </div>
    );
}
```

### 3. Vue 3 예제

```javascript
import { ref, onMounted, onUnmounted } from 'vue';

export function useWebSocket(userId) {
    const notifications = ref([]);
    let ws = null;

    const connect = () => {
        ws = new WebSocket(`ws://localhost:8080/ws/alerts?userId=${userId}`);

        ws.onopen = () => {
            console.log('WebSocket 연결 성공');
        };

        ws.onmessage = (event) => {
            const data = JSON.parse(event.data);
            console.log('알림 수신:', data);

            // 새 알림 추가
            if (data.type !== 'ALERT_READ_STATUS' && data.type !== 'ALERT_DELETED') {
                notifications.value.unshift(data);
            }

            // 읽음 처리
            if (data.type === 'ALERT_READ_STATUS') {
                const index = notifications.value.findIndex(n => n.data === data.data);
                if (index !== -1) {
                    notifications.value[index].isRead = true;
                }
            }

            // 삭제 처리
            if (data.type === 'ALERT_DELETED') {
                notifications.value = notifications.value.filter(n => n.data !== data.data);
            }
        };

        ws.onerror = (error) => {
            console.error('WebSocket 에러:', error);
        };

        ws.onclose = () => {
            console.log('WebSocket 연결 종료');
        };
    };

    onMounted(() => {
        connect();
    });

    onUnmounted(() => {
        if (ws) {
            ws.close();
        }
    });

    return { notifications };
}
```

## 메시지 타입

### 1. 알림 생성 (CRITICAL, WARNING, INFO)

```json
{
    "type": "CRITICAL",
    "title": "심각",
    "message": "CPU 사용률이 90%를 초과했습니다.",
    "timestamp": "2025-10-22T14:30:00",
    "data": 123
}
```

**필드 설명:**
- `type`: 알림 레벨 (`CRITICAL`, `WARNING`, `INFO`)
- `title`: 알림 제목
- `message`: 알림 메시지
- `timestamp`: 알림 발생 시간 (ISO 8601 형식)
- `data`: 알림 ID (Long)

### 2. 읽음 처리 상태 업데이트

```json
{
    "type": "ALERT_READ_STATUS",
    "title": "알림 읽음 처리",
    "message": "알림이 읽음 처리되었습니다.",
    "timestamp": "2025-10-22T14:31:00",
    "data": 123
}
```

**용도**: 사용자가 알림을 읽었을 때 실시간으로 UI 업데이트

### 3. 알림 삭제

```json
{
    "type": "ALERT_DELETED",
    "title": "알림 삭제",
    "message": "알림이 삭제되었습니다.",
    "timestamp": "2025-10-22T14:32:00",
    "data": 123
}
```

**용도**: 사용자가 알림을 삭제했을 때 실시간으로 UI에서 제거

## REST API 연동

WebSocket과 함께 REST API를 사용하여 알림 관리:

### 1. 읽지 않은 알림 조회
```http
GET /api/alerts/unread
Authorization: Bearer {JWT_TOKEN}
```

### 2. 모든 알림 조회
```http
GET /api/alerts
Authorization: Bearer {JWT_TOKEN}
```

### 3. 특정 알림 조회
```http
GET /api/alerts/{id}
Authorization: Bearer {JWT_TOKEN}
```

### 4. 알림 읽음 처리
```http
PATCH /api/alerts/{id}/read
Authorization: Bearer {JWT_TOKEN}
```
**WebSocket**: 읽음 처리 후 `ALERT_READ_STATUS` 메시지 전송

### 5. 알림 삭제
```http
DELETE /api/alerts/{id}
Authorization: Bearer {JWT_TOKEN}
```
**WebSocket**: 삭제 후 `ALERT_DELETED` 메시지 전송

### 6. 모든 알림 읽음 처리
```http
PATCH /api/alerts/read-all
Authorization: Bearer {JWT_TOKEN}
```

### 7. 알림 생성 (테스트용)
```http
POST /api/alerts
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
    "ruleId": 1,
    "containerId": 1,
    "message": "테스트 알림입니다",
    "metricType": "CPU",
    "alertLevel": "CRITICAL"
}
```
**WebSocket**: 생성 후 해당 사용자에게 알림 자동 전송

## 재연결 로직

WebSocket 연결이 끊어졌을 때 자동 재연결 구현 예제:

```javascript
class WebSocketClient {
    constructor(userId) {
        this.userId = userId;
        this.ws = null;
        this.reconnectInterval = 3000; // 3초
        this.maxReconnectAttempts = 5;
        this.reconnectAttempts = 0;
    }

    connect() {
        this.ws = new WebSocket(`ws://localhost:8080/ws/alerts?userId=${this.userId}`);

        this.ws.onopen = () => {
            console.log('WebSocket 연결 성공');
            this.reconnectAttempts = 0; // 재연결 시도 횟수 초기화
        };

        this.ws.onmessage = (event) => {
            const data = JSON.parse(event.data);
            this.handleMessage(data);
        };

        this.ws.onerror = (error) => {
            console.error('WebSocket 에러:', error);
        };

        this.ws.onclose = () => {
            console.log('WebSocket 연결 종료');
            this.reconnect();
        };
    }

    reconnect() {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            console.log(`재연결 시도 ${this.reconnectAttempts}/${this.maxReconnectAttempts}`);

            setTimeout(() => {
                this.connect();
            }, this.reconnectInterval);
        } else {
            console.error('최대 재연결 시도 횟수 초과');
        }
    }

    handleMessage(data) {
        // 메시지 처리 로직
        console.log('메시지 수신:', data);
    }

    disconnect() {
        if (this.ws) {
            this.ws.close();
        }
    }
}

// 사용 예제
const wsClient = new WebSocketClient(1);
wsClient.connect();
```

## 보안 고려사항

### 현재 구현
- `userId`를 query parameter로 전달
- JWT 인증 없이 WebSocket 연결 가능

### 프로덕션 권장사항
1. **JWT 토큰 인증 추가**
   - WebSocket handshake 시 JWT 토큰 검증
   - 또는 첫 메시지로 JWT 토큰 전송 후 검증

2. **CORS 설정 수정**
   - 현재: 모든 origin 허용 (`*`)
   - 권장: 프론트엔드 도메인만 허용

3. **WSS 프로토콜 사용**
   - HTTPS 환경에서는 반드시 `wss://` 사용

## 테스트

### 테스트 HTML 파일 사용
프로젝트 루트에 `websocket-test.html` 파일이 제공됩니다.

1. 서버 실행: `./gradlew bootRun`
2. 브라우저에서 `websocket-test.html` 파일 열기
3. User ID 입력 후 "연결" 버튼 클릭
4. JWT 토큰 입력 후 "알림 생성 및 전송" 버튼으로 테스트

### Postman/Insomnia 테스트
1. New WebSocket Request 생성
2. URL: `ws://localhost:8080/ws/alerts?userId=1`
3. Connect 클릭
4. REST API로 알림 생성하여 WebSocket 메시지 확인

## 예상 시나리오

### 시나리오 1: 컨테이너 CPU 임계치 초과
1. Agent가 컨테이너 메트릭 수집
2. AlertRule에 의해 CPU 90% 초과 감지
3. `AlertService.createAndSendAlert()` 호출
4. DB에 알림 저장
5. **WebSocket으로 실시간 전송**
6. 프론트엔드가 알림 수신 및 표시

### 시나리오 2: 사용자가 알림 읽음 처리
1. 사용자가 알림 클릭
2. `PATCH /api/alerts/{id}/read` 호출
3. DB 업데이트
4. **WebSocket으로 `ALERT_READ_STATUS` 전송**
5. 프론트엔드가 UI 업데이트 (읽음 표시)

### 시나리오 3: 관리자 브로드캐스트
1. 관리자가 중요 공지사항 전송
2. `POST /api/alerts/broadcast` 호출
3. **모든 연결된 사용자에게 WebSocket 전송**
4. 모든 사용자의 화면에 알림 표시

## 문제 해결

### 1. 연결이 안 될 때
- 서버가 실행 중인지 확인
- WebSocket 엔드포인트 URL 확인
- 브라우저 콘솔에서 에러 메시지 확인
- CORS 설정 확인

### 2. 메시지가 수신되지 않을 때
- `userId`가 올바른지 확인
- 서버 로그에서 WebSocket 전송 로그 확인
- 알림이 실제로 생성되었는지 REST API로 확인

### 3. 연결이 자주 끊길 때
- 네트워크 상태 확인
- 서버 로그에서 에러 확인
- 재연결 로직 구현

## 추가 리소스

- [Spring WebSocket 공식 문서](https://docs.spring.io/spring-framework/reference/web/websocket.html)
- [MDN WebSocket API](https://developer.mozilla.org/en-US/docs/Web/API/WebSocket)
- [WebSocket RFC 6455](https://datatracker.ietf.org/doc/html/rfc6455)
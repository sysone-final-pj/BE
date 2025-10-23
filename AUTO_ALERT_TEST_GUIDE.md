# 자동 알림 시스템 테스트 가이드

## 개요
이 가이드는 컨테이너 메트릭 기반 자동 알림 시스템을 테스트하는 방법을 설명합니다.

## 사전 준비

### 1. 서버 실행
```bash
./gradlew bootRun
```

### 2. WebSocket 연결
`websocket-test.html` 파일을 브라우저로 열고:
1. User ID 입력 (예: 1)
2. "연결" 버튼 클릭
3. 연결 성공 확인

### 3. JWT 토큰 발급

**로그인 API 호출**:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"accountId":"testuser","password":"password123"}'
```

응답에서 `accessToken` 복사

## 테스트 단계

### Step 1: 기본 알림 규칙 생성

**API**: `POST /api/test/alerts/rules/create-defaults`

```bash
curl -X POST http://localhost:8080/api/test/alerts/rules/create-defaults
```

**생성되는 규칙**:
- **CPU 알림**: WARNING 70%, CRITICAL 90%
- **Memory 알림**: WARNING 75%, CRITICAL 85%
- **쿨다운**: 60초 (테스트용)

**또는 Swagger UI 사용**:
1. http://localhost:8080/swagger-ui.html 접속
2. `Alert Test` 섹션 열기
3. `POST /api/test/alerts/rules/create-defaults` 실행

---

### Step 2: 컨테이너 목록 확인

**API**: `GET /api/test/alerts/containers`

```bash
curl http://localhost:8080/api/test/alerts/containers
```

**응답 예시**:
```json
{
  "status": 200,
  "message": "컨테이너 2개 조회 완료",
  "data": [
    {
      "id": 1,
      "name": "my-app-container",
      "cpuPercent": 45.20,
      "memPercent": 60.50,
      "memberId": 1
    },
    {
      "id": 2,
      "name": "db-container",
      "cpuPercent": 30.00,
      "memPercent": 50.00,
      "memberId": 1
    }
  ]
}
```

**컨테이너 ID 확인**: 위 응답에서 `id` 값을 메모 (예: 1)

---

### Step 3: 컨테이너 메트릭 조작 - WARNING 테스트

**API**: `POST /api/test/alerts/containers/{containerId}/metrics`

**CPU WARNING 알림 발생** (70% 이상):
```bash
curl -X POST "http://localhost:8080/api/test/alerts/containers/1/metrics?cpuPercent=75"
```

**Memory WARNING 알림 발생** (75% 이상):
```bash
curl -X POST "http://localhost:8080/api/test/alerts/containers/1/metrics?memPercent=80"
```

**예상 결과**:
- WebSocket으로 WARNING 알림 수신
- 메시지: `[경고] 컨테이너 'my-app-container'의 CPU 사용률이 임계값을 초과했습니다. (현재: 75.00%, 임계값: 70%)`

---

### Step 4: 컨테이너 메트릭 조작 - CRITICAL 테스트

**CPU CRITICAL 알림 발생** (90% 이상):
```bash
curl -X POST "http://localhost:8080/api/test/alerts/containers/1/metrics?cpuPercent=95"
```

**Memory CRITICAL 알림 발생** (85% 이상):
```bash
curl -X POST "http://localhost:8080/api/test/alerts/containers/1/metrics?memPercent=90"
```

**예상 결과**:
- WebSocket으로 CRITICAL 알림 수신
- 메시지: `[심각] 컨테이너 'my-app-container'의 CPU 사용률이 임계값을 초과했습니다. (현재: 95.00%, 임계값: 90%)`

---

### Step 5: 중복 알림 방지 테스트 (쿨다운)

같은 컨테이너에 대해 **60초 이내**에 다시 메트릭 설정:

```bash
curl -X POST "http://localhost:8080/api/test/alerts/containers/1/metrics?cpuPercent=98"
```

**예상 결과**:
- ❌ 알림 **발생하지 않음** (쿨다운 기간 중)
- 서버 로그: `쿨다운 기간 중 알림 스킵`

**60초 후 다시 시도**:
```bash
# 60초 기다린 후
curl -X POST "http://localhost:8080/api/test/alerts/containers/1/metrics?cpuPercent=98"
```

**예상 결과**:
- ✅ 알림 다시 발생 (쿨다운 만료)

---

### Step 6: 여러 메트릭 동시 설정

CPU와 Memory를 동시에 임계값 초과하도록 설정:

```bash
curl -X POST "http://localhost:8080/api/test/alerts/containers/1/metrics?cpuPercent=92&memPercent=88"
```

**예상 결과**:
- ✅ CPU CRITICAL 알림 수신
- ✅ Memory CRITICAL 알림 수신
- 총 **2개의 알림** 동시 발생

---

### Step 7: 알림 내역 확인

**읽지 않은 알림 조회**:
```bash
curl -X GET http://localhost:8080/api/alerts/unread \
  -H "Authorization: Bearer {YOUR_JWT_TOKEN}"
```

**모든 알림 조회**:
```bash
curl -X GET http://localhost:8080/api/alerts \
  -H "Authorization: Bearer {YOUR_JWT_TOKEN}"
```

---

## Postman/Insomnia 테스트 시나리오

### 1. 알림 규칙 생성
- **Method**: POST
- **URL**: `http://localhost:8080/api/test/alerts/rules/create-defaults`
- **Expected**: `200 OK`, "기본 알림 규칙 생성 완료"

### 2. 컨테이너 목록 조회
- **Method**: GET
- **URL**: `http://localhost:8080/api/test/alerts/containers`
- **Expected**: 컨테이너 목록 (ID 확인)

### 3. CPU WARNING 테스트
- **Method**: POST
- **URL**: `http://localhost:8080/api/test/alerts/containers/1/metrics?cpuPercent=75`
- **Expected**: WebSocket에서 WARNING 알림 수신

### 4. CPU CRITICAL 테스트
- **Method**: POST
- **URL**: `http://localhost:8080/api/test/alerts/containers/1/metrics?cpuPercent=95`
- **Expected**: WebSocket에서 CRITICAL 알림 수신

### 5. 쿨다운 테스트
- **Method**: POST
- **URL**: `http://localhost:8080/api/test/alerts/containers/1/metrics?cpuPercent=96`
- **Expected**: 60초 이내 재시도 시 알림 없음, 60초 후 재시도 시 알림 발생

---

## 서버 로그 확인

테스트 중 서버 로그에서 다음 메시지 확인:

### 알림 규칙 평가
```
DEBUG - 규칙 평가 시작: containerId=1, ruleId=1
```

### 알림 발생
```
INFO - 알림 발생: containerId=1, ruleId=1, level=CRITICAL, value=95.00
INFO - 알림 생성 및 전송 완료: memberId=1, alertLevel=CRITICAL, message=[심각] 컨테이너...
```

### 쿨다운 스킵
```
DEBUG - 쿨다운 기간 중 알림 스킵: containerId=1, ruleId=1
```

### WebSocket 전송
```
INFO - 알림 전송 성공: userId=1, message=...
```

---

## 트러블슈팅

### 1. 알림이 발생하지 않음
**체크리스트**:
- [ ] AlertRule이 생성되어 있나요? (`GET /api/test/alerts/rules` 확인)
- [ ] AlertRule의 `isEnabled`가 `true`인가요?
- [ ] 컨테이너가 존재하나요? (`GET /api/test/alerts/containers` 확인)
- [ ] 메트릭 값이 임계값을 초과했나요?
- [ ] 쿨다운 기간이 아직 안 지났나요? (60초 대기)

### 2. WebSocket에서 알림을 못 받음
**체크리스트**:
- [ ] WebSocket 연결이 되어 있나요? (websocket-test.html)
- [ ] User ID가 컨테이너 소유자(memberId)와 일치하나요?
- [ ] 서버 로그에서 "알림 전송 성공" 메시지가 있나요?
- [ ] 브라우저 콘솔에서 WebSocket 에러가 있나요?

### 3. 중복 알림이 계속 발생함
**원인**: 쿨다운 기간이 설정되지 않았거나 너무 짧음
**해결**: AlertRule의 `cooldownSeconds` 값 확인 (최소 60초 권장)

---

## 실제 운영 시나리오

실제 Agent와 연동 시:

1. **Agent 등록**: Agent가 컨테이너 메트릭을 5초마다 전송
2. **자동 평가**: `ContainerCollectorService`가 메트릭 저장 후 `AlertRuleEvaluator` 호출
3. **자동 알림**: 임계값 초과 시 자동으로 알림 생성 및 WebSocket 전송
4. **사용자 수신**: 로그인한 사용자가 실시간으로 알림 수신

**테스트 API는 이 흐름을 수동으로 재현**한 것입니다.

---

## 다음 단계

테스트가 성공하면:
1. ✅ 자동 알림 시스템 정상 작동 확인
2. ✅ Agent 연동 시 실제 메트릭으로 자동 알림 발생 예상
3. ✅ 프론트엔드에서 WebSocket 연동하여 실시간 알림 UI 구현

---

## API 요약

| API | Method | 설명 |
|-----|--------|------|
| `/api/test/alerts/rules/create-defaults` | POST | 기본 알림 규칙 생성 |
| `/api/test/alerts/rules` | GET | 활성화된 알림 규칙 조회 |
| `/api/test/alerts/containers` | GET | 컨테이너 목록 조회 |
| `/api/test/alerts/containers/{id}/metrics` | POST | 컨테이너 메트릭 설정 |
| `/api/test/alerts/evaluate-all` | POST | 모든 컨테이너 재평가 |

모든 API는 **Swagger UI**에서도 테스트 가능합니다:
👉 http://localhost:8080/swagger-ui.html
package com.monito.domains.alert.controller;

import com.monito.domains.alert.domain.AlertLevel;
import com.monito.domains.alert.dto.request.AlertCreateRequestDTO;
import com.monito.domains.alert.dto.request.AlertFilterDTO;
import com.monito.domains.alert.dto.request.AlertSortType;
import com.monito.domains.alert.dto.response.AlertDetailResponseDTO;
import com.monito.domains.alert.dto.response.AlertListItemResponseDTO;
import com.monito.domains.alert.service.AlertService;
import com.monito.domains.container.domain.MetricType;
import com.monito.domains.container.dto.request.QuickRangeType;
import com.monito.global.common.response.ApiResponse;
import com.monito.global.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알림(Alert) API Controller
 *
 * <p><b>역할:</b></p>
 * <ul>
 *   <li>시스템 모니터링 알림의 CRUD 및 상태 관리 API 제공</li>
 *   <li>HTTP 요청을 받아 Service 계층으로 전달하고 응답 반환 (Presentation Layer)</li>
 *   <li>RESTful API 설계 원칙 준수 (Resource-based URL, HTTP Method 활용)</li>
 * </ul>
 *
 * <p><b>제공 기능:</b></p>
 * <ul>
 *   <li>알림 조회: 읽지 않은 알림, 전체 알림, 상세 알림, 필터링 조회</li>
 *   <li>알림 상태 관리: 읽음 처리, 전체 읽음 처리</li>
 *   <li>알림 삭제: 개별 삭제, 전체 삭제, 읽은 알림만 삭제</li>
 *   <li>알림 통계: 읽지 않은 알림 개수 (배지용)</li>
 *   <li>고급 필터링: 알림 레벨, 메트릭 타입, 컨테이너명, 시간 범위, 정렬 등</li>
 * </ul>
 *
 * <p><b>계층화 아키텍처 (Layered Architecture):</b></p>
 * <pre>
 * Controller (Presentation) → Service (Business Logic) → Repository (Data Access)
 * </pre>
 * <ul>
 *   <li>각 계층은 인접한 계층하고만 통신 (의존성 방향 단방향)</li>
 *   <li>관심사의 분리 (Separation of Concerns): 각 계층은 자신의 책임만 수행</li>
 *   <li>유지보수성, 테스트 용이성, 확장성 향상</li>
 * </ul>
 */
@Tag(name = "Alert", description = "알림 관련 API")
// Swagger/OpenAPI 문서에서 API를 그룹화하고 설명 추가
// 장점: API 문서 자동 생성, 프론트엔드 개발자와 협업 용이

@Slf4j
// Lombok이 자동으로 Logger 인스턴스를 생성 (private static final Logger log = ...)
// 장점: 로깅 코드 간결화, 로그 프레임워크 교체 용이 (SLF4J 추상화)

@RestController
// @Controller + @ResponseBody의 결합
// 모든 메서드의 반환 값이 HTTP Response Body에 직접 작성됨 (JSON 직렬화)
// 장점: RESTful API 개발에 최적화, @ResponseBody를 매번 작성할 필요 없음

@RequestMapping("/api/alerts")
// 이 컨트롤러의 모든 엔드포인트는 /api/alerts로 시작
// 장점: URL 중복 제거, 일관된 API 경로 관리

@RequiredArgsConstructor
// Lombok이 final 필드에 대한 생성자를 자동 생성
// Spring의 생성자 주입 패턴 구현 (Constructor Injection)
// 장점:
// - 불변성 보장: final 필드는 한 번만 초기화 가능
// - 순환 참조 방지: 컴파일 타임에 순환 의존성 감지
// - 테스트 용이성: Mock 객체 주입 쉬움
public class AlertController {

    /**
     * 알림 서비스
     * <p>의존성 주입 (Dependency Injection) 패턴</p>
     * <ul>
     *   <li>final 키워드로 불변성 보장</li>
     *   <li>@RequiredArgsConstructor를 통한 생성자 주입</li>
     *   <li>인터페이스 타입으로 선언하여 느슨한 결합 (Loose Coupling) 달성</li>
     * </ul>
     */
    private final AlertService alertService;

    /**
     * 읽지 않은 알림 개수 조회 API
     *
     * <p><b>기능:</b></p>
     * <ul>
     *   <li>현재 로그인한 사용자의 읽지 않은 알림 개수 조회</li>
     *   <li>UI 배지나 알림 아이콘에 표시할 숫자 제공</li>
     *   <li>가볍고 빠른 집계 쿼리 (COUNT 사용)</li>
     * </ul>
     *
     * <p><b>인증 및 보안:</b></p>
     * <ul>
     *   <li>@AuthenticationPrincipal: Spring Security의 인증된 사용자 정보 주입</li>
     *   <li>현재 로그인한 사용자의 알림만 조회 (데이터 격리)</li>
     * </ul>
     *
     * @param userDetails Spring Security에서 주입하는 인증된 사용자 정보
     * @return 읽지 않은 알림 개수
     */
    @Operation(summary = "읽지 않은 알림 개수 조회", description = "현재 사용자의 읽지 않은 알림 개수를 조회합니다. (배지 표시용)")
    @GetMapping("/unread/count")
    // HTTP GET 메서드 매핑, URL: /api/alerts/unread/count
    // RESTful 원칙: GET은 데이터 조회(멱등성), 상태 변경 없음
    public ApiResponse<Long> getUnreadAlertCount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
            // @AuthenticationPrincipal: Spring Security가 인증된 사용자 정보를 자동으로 주입
            // 장점: 명시적인 세션 관리 불필요, 타입 안전성
        long count = alertService.getUnreadAlertCount(userDetails.getId());
        return ApiResponse.ok(count, "읽지 않은 알림 개수 조회 성공");
    }

    /**
     * 읽지 않은 알림 목록 조회 API
     *
     * <p><b>기능:</b></p>
     * <ul>
     *   <li>현재 사용자의 읽지 않은 알림만 목록으로 조회</li>
     *   <li>알림 드롭다운이나 알림 센터에 표시할 데이터 제공</li>
     *   <li>리스트 아이템 형식 (요약 정보만 포함)</li>
     * </ul>
     *
     * @param userDetails Spring Security에서 주입하는 인증된 사용자 정보
     * @return 읽지 않은 알림 목록 (요약 정보)
     */
    @Operation(summary = "읽지 않은 알림 목록 조회", description = "현재 사용자의 읽지 않은 알림 목록을 조회합니다.")
    @GetMapping("/unread")
    public ApiResponse<List<AlertListItemResponseDTO>> getUnreadAlerts(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<AlertListItemResponseDTO> alerts = alertService.getUnreadAlertsAsResponse(userDetails.getId());
        return ApiResponse.ok(alerts, "읽지 않은 알림 조회 성공");
    }

    /**
     * 모든 알림 목록 조회 API
     *
     * <p><b>기능:</b></p>
     * <ul>
     *   <li>현재 사용자의 모든 알림 조회 (읽음 + 안읽음)</li>
     *   <li>알림 히스토리 페이지에서 사용</li>
     *   <li>읽음 상태 포함하여 표시</li>
     * </ul>
     *
     * @param userDetails Spring Security에서 주입하는 인증된 사용자 정보
     * @return 모든 알림 목록 (읽음 상태 포함)
     */
    @Operation(summary = "모든 알림 목록 조회", description = "현재 사용자의 모든 알림 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<AlertListItemResponseDTO>> getAllAlerts(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<AlertListItemResponseDTO> alerts = alertService.getAllAlertsAsResponse(userDetails.getId());
        return ApiResponse.ok(alerts, "알림 조회 성공");
    }

    /**
     * 알림 읽음 처리 API
     *
     * <p><b>기능:</b></p>
     * <ul>
     *   <li>특정 알림을 읽음 상태로 변경</li>
     *   <li>본인 소유 알림만 수정 가능 (권한 검증)</li>
     *   <li>부분 수정이므로 PATCH 메서드 사용</li>
     * </ul>
     *
     * <p><b>RESTful API 패턴:</b></p>
     * <ul>
     *   <li>PATCH 메서드: 리소스의 일부만 수정 (읽음 상태만 변경)</li>
     *   <li>Path Variable: URL에 리소스 ID 포함 (/alerts/{id}/read)</li>
     * </ul>
     *
     * @param alertId 읽음 처리할 알림 ID
     * @param userDetails Spring Security에서 주입하는 인증된 사용자 정보
     * @return 성공 응답
     */
    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 변경합니다. (본인 알림만 가능)")
    @PatchMapping("/{id}/read")
    // HTTP PATCH 메서드 매핑, URL: /api/alerts/{id}/read
    // RESTful 원칙: PATCH는 리소스의 부분 수정 (읽음 상태만 변경)
    public ApiResponse<Void> markAsRead(
            @PathVariable("id") Long alertId,
            // @PathVariable: URL 경로의 {id}를 메서드 파라미터로 매핑
            // 장점: 리소스 식별자를 URL에 명시적으로 표현 (RESTful)
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        alertService.markAsRead(alertId, userDetails.getId());
        return ApiResponse.ok("알림이 읽음 처리되었습니다.");

    }

    /**
     * 특정 알림 상세 조회 API
     *
     * <p><b>기능:</b></p>
     * <ul>
     *   <li>알림 ID로 특정 알림의 상세 정보 조회</li>
     *   <li>목록 조회보다 더 많은 상세 정보 포함</li>
     *   <li>본인 소유 알림만 조회 가능 (권한 검증)</li>
     * </ul>
     *
     * <p><b>RESTful API 패턴:</b></p>
     * <ul>
     *   <li>GET /alerts/{id}: 단일 리소스 조회 패턴</li>
     *   <li>Path Variable로 리소스 식별자 전달</li>
     * </ul>
     *
     * @param alertId 조회할 알림 ID
     * @param userDetails Spring Security에서 주입하는 인증된 사용자 정보
     * @return 알림 상세 정보
     */
    @Operation(summary = "특정 알림 상세 조회", description = "알림 ID로 특정 알림의 상세 정보를 조회합니다. (본인 알림만 가능)")
    @GetMapping("/{id}")
    public ApiResponse<AlertDetailResponseDTO> getAlert(
            @PathVariable("id") Long alertId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        AlertDetailResponseDTO response = alertService.getAlert(alertId, userDetails.getId());
        return ApiResponse.ok(response, "알림 조회 성공");
    }

    /**
     * 알림 삭제 API
     *
     * <p><b>기능:</b></p>
     * <ul>
     *   <li>특정 알림을 영구적으로 삭제</li>
     *   <li>본인 소유 알림만 삭제 가능 (권한 검증)</li>
     *   <li>소프트 삭제가 아닌 하드 삭제 (DB에서 완전 제거)</li>
     * </ul>
     *
     * <p><b>RESTful API 패턴:</b></p>
     * <ul>
     *   <li>DELETE 메서드: 리소스 삭제 (멱등성)</li>
     *   <li>Path Variable로 삭제할 리소스 지정</li>
     * </ul>
     *
     * @param alertId 삭제할 알림 ID
     * @param userDetails Spring Security에서 주입하는 인증된 사용자 정보
     * @return 성공 응답
     */
    @Operation(summary = "알림 삭제", description = "특정 알림을 삭제합니다. (본인 알림만 가능)")
    @DeleteMapping("/{id}")
    // HTTP DELETE 메서드 매핑, URL: /api/alerts/{id}
    // RESTful 원칙: DELETE는 리소스 삭제, 멱등성 보장 (여러 번 호출해도 결과 동일)
    public ApiResponse<Void> deleteAlert(
            @PathVariable("id") Long alertId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        alertService.deleteAlert(alertId, userDetails.getId());
        return ApiResponse.ok("알림이 삭제되었습니다.");
    }

    /**
     * 모든 알림 읽음 처리 API
     *
     * <p><b>기능:</b></p>
     * <ul>
     *   <li>현재 사용자의 모든 읽지 않은 알림을 일괄 읽음 처리</li>
     *   <li>알림 센터 "모두 읽음" 버튼에서 사용</li>
     *   <li>배치 업데이트로 효율적 처리</li>
     * </ul>
     *
     * @param userDetails Spring Security에서 주입하는 인증된 사용자 정보
     * @return 성공 응답
     */
    @Operation(summary = "모든 알림 읽음 처리", description = "사용자의 모든 알림을 읽음 처리합니다.")
    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(@AuthenticationPrincipal CustomUserDetails userDetails) {
        alertService.markAllAsRead(userDetails.getId());
        return ApiResponse.ok("모든 알림이 읽음 처리되었습니다.");
    }

    /**
     * 모든 알림 삭제 API
     *
     * <p><b>기능:</b></p>
     * <ul>
     *   <li>현재 사용자의 모든 알림을 일괄 삭제</li>
     *   <li>읽음/안읽음 상태 관계없이 모두 삭제</li>
     *   <li>주의: 복구 불가능한 영구 삭제</li>
     * </ul>
     *
     * @param userDetails Spring Security에서 주입하는 인증된 사용자 정보
     * @return 성공 응답
     */
    @Operation(summary = "모든 알림 삭제", description = "사용자의 모든 알림을 삭제합니다.")
    @DeleteMapping("/all")
    public ApiResponse<Void> deleteAllAlerts(@AuthenticationPrincipal CustomUserDetails userDetails) {
        alertService.deleteAllAlerts(userDetails.getId());
        return ApiResponse.ok("모든 알림이 삭제되었습니다.");
    }

    /**
     * 읽은 알림 모두 삭제 API
     *
     * <p><b>기능:</b></p>
     * <ul>
     *   <li>현재 사용자의 읽은 알림만 일괄 삭제</li>
     *   <li>읽지 않은 알림은 보존</li>
     *   <li>알림 관리를 위한 선택적 정리 기능</li>
     * </ul>
     *
     * @param userDetails Spring Security에서 주입하는 인증된 사용자 정보
     * @return 성공 응답
     */
    @Operation(summary = "읽은 알림 모두 삭제", description = "사용자의 읽은 알림을 모두 삭제합니다.")
    @DeleteMapping("/read")
    public ApiResponse<Void> deleteReadAlerts(@AuthenticationPrincipal CustomUserDetails userDetails) {
        alertService.deleteReadAlerts(userDetails.getId());
        return ApiResponse.ok("읽은 알림이 모두 삭제되었습니다.");
    }

    /**
     * 필터 조건에 따른 알림 조회 API
     *
     * <p><b>기능:</b></p>
     * <ul>
     *   <li>다양한 조건으로 알림을 필터링하고 정렬</li>
     *   <li>동적 쿼리 생성을 통한 유연한 검색</li>
     *   <li>복잡한 검색 조건 조합 가능 (AND 조건)</li>
     * </ul>
     *
     * <p><b>주요 필터링 옵션:</b></p>
     * <ul>
     *   <li>alertLevel: 알림 심각도 (CRITICAL, HIGH, WARNING, INFO)</li>
     *   <li>metricType: 메트릭 종류 (CPU, Memory, Disk, Network 등)</li>
     *   <li>agentName/containerName: 대상 시스템 필터 (부분 일치 검색)</li>
     *   <li>quickRangeType: 빠른 시간 범위 선택 (최근 5분, 1시간 등)</li>
     *   <li>collectedAtFrom/To: 사용자 정의 시간 범위</li>
     *   <li>createdAtFrom/To: 알림 생성 시간 범위</li>
     *   <li>isRead: 읽음 상태 필터</li>
     *   <li>sortBy: 정렬 기준 (다중 필드 지원)</li>
     * </ul>
     *
     * <p><b>디자인 패턴:</b></p>
     * <ul>
     *   <li>Builder 패턴: AlertFilterDTO를 통한 복잡한 객체 생성</li>
     *   <li>Query Parameter 패턴: URL 쿼리 스트링으로 필터 조건 전달</li>
     *   <li>Strategy 패턴: 동적 정렬 기준 적용</li>
     * </ul>
     *
     * @param userDetails Spring Security에서 주입하는 인증된 사용자 정보
     * @param alertLevel 알림 레벨 필터 (선택)
     * @param metricType 메트릭 타입 필터 (선택)
     * @param agentName 에이전트 이름 필터 (선택, 부분 일치)
     * @param containerName 컨테이너 이름 필터 (선택, 부분 일치)
     * @param quickRangeType 빠른 시간 범위 선택 (선택, 이 값이 있으면 collectedAtFrom/To 무시)
     * @param collectedAtFrom 수집 시작 시간 (선택, ISO 8601 형식)
     * @param collectedAtTo 수집 종료 시간 (선택, ISO 8601 형식)
     * @param createdAtFrom 생성 시작 시간 (선택, ISO 8601 형식)
     * @param createdAtTo 생성 종료 시간 (선택, ISO 8601 형식)
     * @param isRead 읽음 여부 필터 (선택)
     * @param sortBy 정렬 기준 (선택, 기본값: CREATED_AT)
     * @return 필터링된 알림 목록
     */
    @Operation(
            summary = "필터 조건으로 알림 조회",
            description = """
                    알림을 다양한 조건으로 필터링하고 정렬합니다.

                    **사용 가능한 필터:**
                    - alertLevel: 경고 레벨 (CRITICAL, HIGH, WARNING, INFO)
                    - metricType: 메트릭 타입 (CPU_PERCENT, MEM_PERCENT, DISK_USAGE_GB, NETWORK_TOTAL_BYTES 등)
                    - agentName: 에이전트 이름 (부분 일치)
                    - containerName: 컨테이너 이름 (부분 일치)
                    - quickRangeType: 빠른 시간 범위 선택 (LAST_5_MINUTES, LAST_10_MINUTES 등)
                      * quickRangeType이 있으면 collectedAtFrom/To는 무시됨
                    - collectedAtFrom/To: 수집 시간 범위 (예: 2024-10-01T00:00:00) - quickRangeType이 없을 때 사용
                    - createdAtFrom/To: 생성 시간 범위 (예: 2024-10-01T00:00:00)
                    - isRead: 읽음 여부 (true/false)
                    - sortBy: 정렬 기준 (ALERT_LEVEL, METRIC_TYPE, CONTAINER_NAME, METRIC_VALUE, COLLECTED_AT, CREATED_AT)
                    """
    )
    @GetMapping("/filter")
    // HTTP GET 메서드 매핑, URL: /api/alerts/filter
    // RESTful 원칙: 복잡한 조회 조건은 /filter 엔드포인트로 분리
    public ApiResponse<List<AlertListItemResponseDTO>> getAlertsWithFilter(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            // @AuthenticationPrincipal: Spring Security가 인증된 사용자 정보를 자동으로 주입

            @Parameter(description = "경고 레벨 (CRITICAL, HIGH, WARNING, INFO)")
            @RequestParam(required = false) AlertLevel alertLevel,
            // @RequestParam: URL 쿼리 파라미터를 메서드 파라미터로 바인딩
            // required = false: 선택적 파라미터, 값이 없어도 null로 처리
            // 장점: 타입 안전성, 자동 변환 (String → Enum)

            @Parameter(description = "메트릭 타입 (CPU_PERCENT, MEM_PERCENT, DISK_USAGE_GB, NETWORK_TOTAL_BYTES 등)")
            @RequestParam(required = false) MetricType metricType,

            @Parameter(description = "에이전트 이름 (부분 일치)")
            @RequestParam(required = false) String agentName,

            @Parameter(description = "컨테이너 이름 (부분 일치)")
            @RequestParam(required = false) String containerName,

            @Parameter(description = "빠른 시간 범위 선택 (LAST_5_MINUTES, LAST_10_MINUTES, LAST_30_MINUTES, LAST_1_HOUR, LAST_3_HOURS, LAST_6_HOURS, LAST_12_HOURS, LAST_24_HOURS). 이 값이 있으면 collectedAtFrom/To는 무시됨")
            @RequestParam(required = false) QuickRangeType quickRangeType,

            @Parameter(description = "수집 시작 시간 (ISO 8601 형식: 2024-10-01T00:00:00) - quickRangeType이 없을 때 사용")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime collectedAtFrom,
            // @DateTimeFormat: String → LocalDateTime 자동 변환
            // ISO.DATE_TIME: ISO 8601 표준 형식 (yyyy-MM-dd'T'HH:mm:ss)
            // 장점: 날짜 파싱 로직을 Spring이 자동 처리

            @Parameter(description = "수집 종료 시간 (ISO 8601 형식: 2024-10-31T23:59:59) - quickRangeType이 없을 때 사용")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime collectedAtTo,

            @Parameter(description = "생성 시작 시간 (ISO 8601 형식: 2024-10-01T00:00:00)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAtFrom,

            @Parameter(description = "생성 종료 시간 (ISO 8601 형식: 2024-10-31T23:59:59)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAtTo,

            @Parameter(description = "읽음 여부 (true: 읽음, false: 안읽음)")
            @RequestParam(required = false) Boolean isRead,

            @Parameter(description = "정렬 기준 (ALERT_LEVEL, METRIC_TYPE, CONTAINER_NAME, METRIC_VALUE, COLLECTED_AT). 기본값: CREATED_AT")
            @RequestParam(required = false) AlertSortType sortBy
    ) {
        // Builder 패턴을 사용한 DTO 생성
        // 장점: 선택적 파라미터가 많을 때 가독성 향상, 불변 객체 생성
        AlertFilterDTO filter = AlertFilterDTO.builder()
                .alertLevel(alertLevel)
                .metricType(metricType)
                .agentName(agentName)
                .containerName(containerName)
                .quickRangeType(quickRangeType)
                .collectedAtFrom(collectedAtFrom)
                .collectedAtTo(collectedAtTo)
                .createdAtFrom(createdAtFrom)
                .createdAtTo(createdAtTo)
                .isRead(isRead)
                .sortType(sortBy)
                .build();

        List<AlertListItemResponseDTO> alerts = alertService.getAlertsWithFilter(userDetails.getId(), filter);
        return ApiResponse.ok(alerts, "필터링된 알림 조회 성공");
    }
}
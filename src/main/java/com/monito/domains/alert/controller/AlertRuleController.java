package com.monito.domains.alert.controller;

import com.monito.domains.alert.dto.request.AlertRuleCreateRequestDTO;
import com.monito.domains.alert.dto.request.AlertRuleUpdateRequestDTO;
import com.monito.domains.alert.dto.response.AlertRuleResponseDTO;
import com.monito.domains.alert.service.AlertRuleService;
import com.monito.global.common.response.ApiResponse;
import com.monito.global.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 알림 규칙(Alert Rule) API Controller
 *
 * <p><b>역할:</b></p>
 * <ul>
 *   <li>알림 규칙의 생성, 조회, 수정, 삭제 API 제공 (Full CRUD)</li>
 *   <li>HTTP 요청을 받아 Service 계층으로 전달하고 응답 반환 (Presentation Layer)</li>
 *   <li>RESTful API 설계 원칙 준수 (Resource-based URL, HTTP Method 활용)</li>
 * </ul>
 *
 * <p><b>제공 기능:</b></p>
 * <ul>
 *   <li>알림 규칙 생성: 메트릭 임계값 기반 알림 조건 설정</li>
 *   <li>알림 규칙 조회: 개별 조회, 전체 목록 조회</li>
 *   <li>알림 규칙 수정: 조건, 임계값, 알림 레벨 등 변경</li>
 *   <li>알림 규칙 활성화/비활성화: 규칙 적용 여부 토글</li>
 *   <li>알림 규칙 삭제: 규칙 영구 제거</li>
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
 *
 * <p><b>비즈니스 로직:</b></p>
 * <ul>
 *   <li>알림 규칙은 컨테이너 메트릭 모니터링의 핵심 기능</li>
 *   <li>사용자가 설정한 임계값을 초과하면 자동으로 알림 생성</li>
 *   <li>각 사용자는 자신만의 알림 규칙을 관리 (멀티테넌시)</li>
 * </ul>
 */
@Tag(name = "Alert Rule", description = "알림 규칙 관리 API")
// Swagger/OpenAPI 문서에서 API를 그룹화하고 설명 추가
// 장점: API 문서 자동 생성, 프론트엔드 개발자와 협업 용이

@Slf4j
// Lombok이 자동으로 Logger 인스턴스를 생성 (private static final Logger log = ...)
// 장점: 로깅 코드 간결화, 로그 프레임워크 교체 용이 (SLF4J 추상화)

@RestController
// @Controller + @ResponseBody의 결합
// 모든 메서드의 반환 값이 HTTP Response Body에 직접 작성됨 (JSON 직렬화)
// 장점: RESTful API 개발에 최적화, @ResponseBody를 매번 작성할 필요 없음

@RequestMapping("/api/alert-rules")
// 이 컨트롤러의 모든 엔드포인트는 /api/alert-rules로 시작
// 장점: URL 중복 제거, 일관된 API 경로 관리
// RESTful 원칙: 복수형 명사로 리소스 표현 (alert-rules)

@RequiredArgsConstructor
// Lombok이 final 필드에 대한 생성자를 자동 생성
// Spring의 생성자 주입 패턴 구현 (Constructor Injection)
// 장점:
// - 불변성 보장: final 필드는 한 번만 초기화 가능
// - 순환 참조 방지: 컴파일 타임에 순환 의존성 감지
// - 테스트 용이성: Mock 객체 주입 쉬움
public class AlertRuleController {

    /**
     * 알림 규칙 서비스
     * <p>의존성 주입 (Dependency Injection) 패턴</p>
     * <ul>
     *   <li>final 키워드로 불변성 보장</li>
     *   <li>@RequiredArgsConstructor를 통한 생성자 주입</li>
     *   <li>인터페이스 타입으로 선언하여 느슨한 결합 (Loose Coupling) 달성</li>
     * </ul>
     */
    private final AlertRuleService alertRuleService;

    /**
     * 알림 규칙 생성 API
     *
     * <p><b>기능:</b></p>
     * <ul>
     *   <li>새로운 알림 규칙을 생성하여 DB에 저장</li>
     *   <li>메트릭 타입, 임계값, 알림 레벨 등을 설정</li>
     *   <li>생성된 규칙은 즉시 모니터링 시스템에 적용</li>
     * </ul>
     *
     * <p><b>RESTful API 패턴:</b></p>
     * <ul>
     *   <li>POST 메서드: 새로운 리소스 생성 (비멱등성)</li>
     *   <li>Request Body: JSON 형식으로 규칙 정보 전달</li>
     *   <li>Response: 생성된 규칙의 전체 정보 반환 (ID 포함)</li>
     * </ul>
     *
     * <p><b>유효성 검증:</b></p>
     * <ul>
     *   <li>@Valid: DTO의 Bean Validation 어노테이션 검증</li>
     *   <li>필수 필드, 값 범위, 형식 등 자동 검증</li>
     *   <li>검증 실패 시 400 Bad Request 자동 반환</li>
     * </ul>
     *
     * @param userDetails Spring Security에서 주입하는 인증된 사용자 정보
     * @param request 알림 규칙 생성 요청 DTO (메트릭 타입, 임계값, 알림 레벨 등)
     * @return 생성된 알림 규칙 정보
     */
    @Operation(summary = "알림 규칙 생성", description = "새로운 알림 규칙을 생성합니다.")
    @PostMapping
    // HTTP POST 메서드 매핑, URL: /api/alert-rules
    // RESTful 원칙: POST는 새로운 리소스 생성, 비멱등성 (여러 번 호출 시 다른 결과)
    public ApiResponse<AlertRuleResponseDTO> createAlertRule(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            // @AuthenticationPrincipal: Spring Security가 인증된 사용자 정보를 자동으로 주입
            @Valid @RequestBody AlertRuleCreateRequestDTO request) {
            // @Valid: Bean Validation 수행 (DTO의 @NotNull, @Min, @Max 등 검증)
            // @RequestBody: HTTP Request Body의 JSON을 DTO 객체로 자동 역직렬화
            // 장점: 유효성 검증 코드를 Controller에서 분리, 선언적 검증
        AlertRuleResponseDTO response = alertRuleService.createAlertRule(userDetails.getId(), request);
        return ApiResponse.ok(response, "알림 규칙이 생성되었습니다.");
    }

    /**
     * 특정 알림 규칙 조회 API
     *
     * <p><b>기능:</b></p>
     * <ul>
     *   <li>알림 규칙 ID로 특정 규칙의 상세 정보 조회</li>
     *   <li>본인 소유 규칙만 조회 가능 (권한 검증)</li>
     *   <li>활성화/비활성화 상태 포함 모든 정보 반환</li>
     * </ul>
     *
     * <p><b>RESTful API 패턴:</b></p>
     * <ul>
     *   <li>GET /alert-rules/{ruleId}: 단일 리소스 조회 패턴</li>
     *   <li>Path Variable로 리소스 식별자 전달</li>
     * </ul>
     *
     * @param ruleId 조회할 알림 규칙 ID
     * @param userDetails Spring Security에서 주입하는 인증된 사용자 정보
     * @return 알림 규칙 상세 정보
     */
    @Operation(summary = "특정 알림 규칙 조회", description = "알림 규칙 ID로 특정 알림 규칙을 조회합니다. (본인 규칙만 가능)")
    @GetMapping("/{ruleId}")
    // HTTP GET 메서드 매핑, URL: /api/alert-rules/{ruleId}
    // RESTful 원칙: GET은 리소스 조회, 멱등성 보장
    public ApiResponse<AlertRuleResponseDTO> getAlertRule(
            @PathVariable Long ruleId,
            // @PathVariable: URL 경로의 {ruleId}를 메서드 파라미터로 매핑
            // 장점: 리소스 식별자를 URL에 명시적으로 표현 (RESTful)
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        AlertRuleResponseDTO response = alertRuleService.getAlertRule(ruleId, userDetails.getId());
        return ApiResponse.ok(response, "알림 규칙 조회 성공");
    }

    /**
     * 사용자의 모든 알림 규칙 조회 API
     *
     * <p><b>기능:</b></p>
     * <ul>
     *   <li>현재 로그인한 사용자의 모든 알림 규칙 목록 조회</li>
     *   <li>활성화 및 비활성화된 규칙 모두 포함</li>
     *   <li>알림 규칙 관리 페이지에서 사용</li>
     * </ul>
     *
     * <p><b>RESTful API 패턴:</b></p>
     * <ul>
     *   <li>GET /alert-rules: 컬렉션 조회 패턴</li>
     *   <li>현재 사용자 기준으로 자동 필터링 (인증 정보 활용)</li>
     * </ul>
     *
     * @param userDetails Spring Security에서 주입하는 인증된 사용자 정보
     * @return 사용자의 모든 알림 규칙 목록
     */
    @Operation(summary = "모든 알림 규칙 조회", description = "현재 사용자의 모든 알림 규칙을 조회합니다.")
    @GetMapping
    public ApiResponse<List<AlertRuleResponseDTO>> getAllAlertRules(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<AlertRuleResponseDTO> rules = alertRuleService.getAllAlertRules(userDetails.getId());
        return ApiResponse.ok(rules, "알림 규칙 목록 조회 성공");
    }

    /**
     * 알림 규칙 수정 API
     *
     * <p><b>기능:</b></p>
     * <ul>
     *   <li>기존 알림 규칙의 설정 값 수정</li>
     *   <li>임계값, 알림 레벨, 조건 등 변경 가능</li>
     *   <li>본인 소유 규칙만 수정 가능 (권한 검증)</li>
     * </ul>
     *
     * <p><b>RESTful API 패턴:</b></p>
     * <ul>
     *   <li>PATCH 메서드: 리소스의 부분 수정 (일부 필드만 변경)</li>
     *   <li>PUT과의 차이: PUT은 전체 교체, PATCH는 부분 수정</li>
     *   <li>Path Variable로 수정할 리소스 지정</li>
     * </ul>
     *
     * <p><b>유효성 검증:</b></p>
     * <ul>
     *   <li>@Valid: 수정 요청 DTO의 Bean Validation 수행</li>
     *   <li>변경하려는 값의 범위, 형식 검증</li>
     * </ul>
     *
     * @param ruleId 수정할 알림 규칙 ID
     * @param userDetails Spring Security에서 주입하는 인증된 사용자 정보
     * @param request 알림 규칙 수정 요청 DTO
     * @return 수정된 알림 규칙 정보
     */
    @Operation(summary = "알림 규칙 수정", description = "알림 규칙을 수정합니다. (본인 규칙만 가능)")
    @PatchMapping("/{ruleId}")
    // HTTP PATCH 메서드 매핑, URL: /api/alert-rules/{ruleId}
    // RESTful 원칙: PATCH는 리소스의 부분 수정, PUT은 전체 교체
    public ApiResponse<AlertRuleResponseDTO> updateAlertRule(
            @PathVariable Long ruleId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AlertRuleUpdateRequestDTO request) {
        AlertRuleResponseDTO response = alertRuleService.updateAlertRule(
                ruleId, userDetails.getId(), request);
        return ApiResponse.ok(response, "알림 규칙이 수정되었습니다.");
    }

    /**
     * 알림 규칙 활성화/비활성화 API
     *
     * <p><b>기능:</b></p>
     * <ul>
     *   <li>알림 규칙의 활성화 상태를 토글</li>
     *   <li>비활성화 시 알림 생성 중지, 규칙은 보존</li>
     *   <li>활성화 시 즉시 모니터링 재개</li>
     * </ul>
     *
     * <p><b>디자인 패턴:</b></p>
     * <ul>
     *   <li>Command 패턴: 특정 액션을 명시적인 엔드포인트로 분리</li>
     *   <li>/toggle 경로: 상태 변경을 나타내는 의미적 URL</li>
     *   <li>Query Parameter: enabled 값으로 목표 상태 지정</li>
     * </ul>
     *
     * <p><b>비즈니스 로직:</b></p>
     * <ul>
     *   <li>규칙 삭제 없이 일시적으로 알림을 중지하고 싶을 때 사용</li>
     *   <li>유지보수, 테스트 등의 상황에서 유용</li>
     * </ul>
     *
     * @param ruleId 토글할 알림 규칙 ID
     * @param enabled 목표 상태 (true: 활성화, false: 비활성화)
     * @param userDetails Spring Security에서 주입하는 인증된 사용자 정보
     * @return 변경된 알림 규칙 정보
     */
    @Operation(summary = "알림 규칙 활성화/비활성화", description = "알림 규칙을 활성화하거나 비활성화합니다.")
    @PatchMapping("/{ruleId}/toggle")
    // HTTP PATCH 메서드 매핑, URL: /api/alert-rules/{ruleId}/toggle
    // RESTful 원칙: 리소스의 특정 속성(enabled) 변경은 하위 경로로 표현
    public ApiResponse<AlertRuleResponseDTO> toggleAlertRule(
            @PathVariable Long ruleId,
            @RequestParam boolean enabled,
            // @RequestParam: URL 쿼리 파라미터 바인딩 (예: ?enabled=true)
            // boolean 타입이므로 required=true가 기본값
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        AlertRuleResponseDTO response = alertRuleService.toggleAlertRule(
                ruleId, userDetails.getId(), enabled);
        return ApiResponse.ok(response,
                enabled ? "알림 규칙이 활성화되었습니다." : "알림 규칙이 비활성화되었습니다.");
    }

    /**
     * 알림 규칙 삭제 API
     *
     * <p><b>기능:</b></p>
     * <ul>
     *   <li>알림 규칙을 영구적으로 삭제</li>
     *   <li>본인 소유 규칙만 삭제 가능 (권한 검증)</li>
     *   <li>연관된 알림은 보존 (히스토리 유지)</li>
     * </ul>
     *
     * <p><b>RESTful API 패턴:</b></p>
     * <ul>
     *   <li>DELETE 메서드: 리소스 삭제 (멱등성)</li>
     *   <li>Path Variable로 삭제할 리소스 지정</li>
     *   <li>204 No Content 대신 200 OK + 메시지 반환 (사용자 친화적)</li>
     * </ul>
     *
     * <p><b>비즈니스 로직:</b></p>
     * <ul>
     *   <li>일시 중지가 아닌 영구 제거</li>
     *   <li>복구 불가능하므로 신중한 사용 필요</li>
     *   <li>과거에 생성된 알림은 삭제되지 않음 (참조 무결성)</li>
     * </ul>
     *
     * @param ruleId 삭제할 알림 규칙 ID
     * @param userDetails Spring Security에서 주입하는 인증된 사용자 정보
     * @return 성공 응답
     */
    @Operation(summary = "알림 규칙 삭제", description = "알림 규칙을 삭제합니다. (본인 규칙만 가능)")
    @DeleteMapping("/{ruleId}")
    // HTTP DELETE 메서드 매핑, URL: /api/alert-rules/{ruleId}
    // RESTful 원칙: DELETE는 리소스 삭제, 멱등성 보장 (여러 번 호출해도 결과 동일)
    public ApiResponse<Void> deleteAlertRule(
            @PathVariable Long ruleId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        alertRuleService.deleteAlertRule(ruleId, userDetails.getId());
        return ApiResponse.ok("알림 규칙이 삭제되었습니다.");
    }
}

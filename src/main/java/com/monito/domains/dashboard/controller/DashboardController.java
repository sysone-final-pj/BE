package com.monito.domains.dashboard.controller;

import com.monito.domains.container.domain.ContainerHealth;
import com.monito.domains.container.domain.ContainerState;
import com.monito.domains.dashboard.dto.request.ContainerFilterDTO;
import com.monito.domains.dashboard.dto.request.ContainerSortType;
import com.monito.domains.dashboard.dto.request.TimeRange;
import com.monito.domains.dashboard.dto.response.*;
import com.monito.domains.dashboard.dto.response.metrics.AgentContainerCountDTO;
import com.monito.domains.dashboard.dto.response.BlockIOStatsTimeSeriesDTO;
import com.monito.domains.dashboard.dto.response.NetworkStatsTimeSeriesDTO;
import com.monito.domains.dashboard.service.DashboardService;
import com.monito.global.common.response.ApiResponse;

import java.time.LocalDate;
import java.util.List;

import com.monito.global.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 대시보드 API Controller
 *
 * <p><b>역할:</b></p>
 * <ul>
 *   <li>실시간 컨테이너 모니터링 대시보드 API 제공</li>
 *   <li>HTTP 요청을 받아 Service 계층으로 전달하고 응답 반환 (Presentation Layer)</li>
 *   <li>RESTful API 설계 원칙 준수 (Resource-based URL, HTTP Method 활용)</li>
 * </ul>
 *
 * <p><b>제공 기능:</b></p>
 * <ul>
 *   <li>컨테이너 목록 조회 (검색, 필터링, 정렬, 즐겨찾기 기능)</li>
 *   <li>컨테이너 분포 통계 (Agent별 컨테이너 개수 집계)</li>
 *   <li>실시간 메트릭 차트 데이터 조회 (네트워크, Block I/O 시계열 데이터)</li>
 *   <li>컨테이너 상세 메트릭 조회 (CPU, 메모리, 네트워크, 스토리지, 로그 등)</li>
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
@Tag(name = "Dashboard", description = "대시보드 전용 API - 실시간 컨테이너 모니터링 및 통계")
// Swagger/OpenAPI 문서에서 API를 그룹화하고 설명 추가
// 장점: API 문서 자동 생성, 프론트엔드 개발자와 협업 용이

@Slf4j
// Lombok이 자동으로 Logger 인스턴스를 생성 (private static final Logger log = ...)
// 장점: 로깅 코드 간결화, 로그 프레임워크 교체 용이 (SLF4J 추상화)

@RestController
// @Controller + @ResponseBody의 결합
// 모든 메서드의 반환 값이 HTTP Response Body에 직접 작성됨 (JSON 직렬화)
// 장점: RESTful API 개발에 최적화, @ResponseBody를 매번 작성할 필요 없음

@RequestMapping("/api/dashboard")
// 이 컨트롤러의 모든 엔드포인트는 /api/dashboard로 시작
// 장점: URL 중복 제거, 일관된 API 경로 관리

@RequiredArgsConstructor
// Lombok이 final 필드에 대한 생성자를 자동 생성
// Spring의 생성자 주입 패턴 구현 (Constructor Injection)
// 장점:
// - 불변성 보장: final 필드는 한 번만 초기화 가능
// - 순환 참조 방지: 컴파일 타임에 순환 의존성 감지
// - 테스트 용이성: Mock 객체 주입 쉬움
public class DashboardController {

    /**
     * 대시보드 서비스
     * <p>의존성 주입 (Dependency Injection) 패턴</p>
     * <ul>
     *   <li>final 키워드로 불변성 보장</li>
     *   <li>@RequiredArgsConstructor를 통한 생성자 주입</li>
     *   <li>인터페이스 타입으로 선언하여 느슨한 결합 (Loose Coupling) 달성</li>
     * </ul>
     */
    private final DashboardService dashboardService;

    /**
     * 대시보드용 컨테이너 목록 조회 API
     *
     * <p><b>기능:</b></p>
     * <ul>
     *   <li>실시간 컨테이너 목록을 검색, 필터링, 정렬하여 조회</li>
     *   <li>사용자별 즐겨찾기 정보 포함하여 반환</li>
     *   <li>대시보드 카드 뷰에 최적화된 경량 DTO 사용</li>
     * </ul>
     *
     * <p><b>디자인 패턴:</b></p>
     * <ul>
     *   <li>Query Parameter 패턴: 검색/필터 조건을 URL 쿼리 파라미터로 전달</li>
     *   <li>Builder 패턴: ContainerFilterDTO 생성 시 가독성 있는 객체 생성</li>
     *   <li>통일된 응답 형식: ApiResponse로 감싸서 일관성 있는 API 제공</li>
     * </ul>
     *
     * @param keyword 검색 키워드 (컨테이너 이름, 이미지명 등)
     * @param sortBy 정렬 기준 (CPU_PERCENT, MEM_PERCENT, FAVORITE)
     * @param favoriteOnly 즐겨찾기만 보기 (true: 즐겨찾기만, false/null: 전체)
     * @param states 상태 필터 (RUNNING, RESTARTING, PAUSED, CREATED, EXIT, DEAD)
     * @param healths 헬스 필터 (HEALTHY, UNHEALTHY, STARTING, NONE, UNKNOWN)
     * @param agentIds 에이전트 ID 필터 (다중 선택 가능)
     * @param userDetails 현재 로그인한 사용자 정보 (Spring Security 자동 주입)
     * @return 필터링 + 정렬된 컨테이너 카드 목록
     */
    @Operation(summary = "컨테이너 목록조회(검색+필터+정렬)",
            description = """
                    컨테이너 목록 조회 with 검색 + 필터 + 정렬 (경량화된 카드 DTO 사용, 즐겨찾기 여부 포함)

                    **검색 옵션:**
                    - keyword: 검색 키워드 (컨테이너 이름, 이미지명 검색)

                    **필터 옵션:**
                    - favoriteOnly: 즐겨찾기만 보기 (true/false)
                    - states: 상태 필터 (RUNNING, RESTARTING, PAUSED, CREATED, EXIT, DEAD)
                    - healths: 헬스 필터 (HEALTHY, UNHEALTHY, STARTING, NONE, UNKNOWN)
                    - agentIds: 에이전트 ID 필터

                    **정렬 옵션:**
                    - sortBy: CPU_PERCENT, MEM_PERCENT, FAVORITE

                    **응답 데이터:**
                    - isFavorite: 즐겨찾기 여부 (true/false) 포함
                    """)
    @GetMapping("/containers")
    // HTTP GET 메서드 매핑, URL: /api/dashboard/containers
    // RESTful 원칙: GET은 데이터 조회(멱등성), 상태 변경 없음
    public ApiResponse<List<ContainerCardResponseDTO>> getAllContainers(
            @Parameter(description = "검색 키워드 (컨테이너 이름, 이미지명 등)")
            @RequestParam(required = false) String keyword,
            // @RequestParam: Query String에서 파라미터 추출
            // required = false: 선택적 파라미터 (null 허용)

            @Parameter(description = "정렬 기준 (CPU_PERCENT, MEM_PERCENT, FAVORITE)")
            @RequestParam(required = false) ContainerSortType sortBy,

            @Parameter(description = "즐겨찾기만 보기 (true: 즐겨찾기만, false/null: 전체)")
            @RequestParam(required = false) Boolean favoriteOnly,

            @Parameter(description = "상태 필터 (다중 선택 가능)")
            @RequestParam(required = false) List<ContainerState> states,
            // Spring이 자동으로 여러 값을 List로 변환
            // 예: ?states=RUNNING&states=PAUSED → List.of(RUNNING, PAUSED)

            @Parameter(description = "헬스 필터 (다중 선택 가능)")
            @RequestParam(required = false) List<ContainerHealth> healths,

            @Parameter(description = "에이전트 ID 필터 (다중 선택 가능)")
            @RequestParam(required = false) List<Long> agentIds,

            @AuthenticationPrincipal CustomUserDetails userDetails) {
            // @AuthenticationPrincipal: Spring Security의 인증된 사용자 정보 자동 주입
            // CustomUserDetails: UserDetails를 구현한 사용자 정의 클래스

        // 인증된 사용자의 memberId 추출 (즐겨찾기 조회를 위해 필요)
        Long memberId = userDetails != null ? Long.valueOf(userDetails.getId()) : null;

        // 필터 DTO 생성 (Builder 패턴 사용)
        // 장점: 가독성 향상, 파라미터 순서 무관, null 안전성
        ContainerFilterDTO filter =
                ContainerFilterDTO.builder()
                        .keyword(keyword)
                        .favoriteOnly(favoriteOnly)
                        .states(states)
                        .healths(healths)
                        .agentIds(agentIds)
                        .build();

        log.info("GET /api/dashboard/containers - 대시보드용 컨테이너 목록 조회 (정렬: {}, memberId: {}, keyword: {})", sortBy, memberId, keyword);

        // Service 계층에 비즈니스 로직 위임
        List<ContainerCardResponseDTO> containers = dashboardService.getAllContainers(sortBy, memberId, filter);

        // 통일된 응답 형식으로 반환 (ApiResponse 래퍼 사용)
        return ApiResponse.ok(containers, "대시보드 컨테이너 목록을 성공적으로 조회했습니다.");
    }

    /**
     * Agent별 컨테이너 개수 집계 API
     *
     * <p><b>기능:</b></p>
     * <ul>
     *   <li>각 Agent에 배포된 컨테이너 개수를 집계하여 반환</li>
     *   <li>대시보드의 컨테이너 분포 차트에 사용</li>
     *   <li>개수 내림차순으로 정렬하여 반환</li>
     * </ul>
     *
     * <p><b>사용 사례:</b></p>
     * <ul>
     *   <li>컨테이너 분포 시각화 (파이 차트, 바 차트 등)</li>
     *   <li>Agent별 부하 현황 모니터링</li>
     *   <li>리소스 배분 현황 파악</li>
     * </ul>
     *
     * @return Agent별 컨테이너 개수 목록 (개수 내림차순 정렬)
     */
    @Operation(summary = "컨테이너 분포 조회",
            description = "agent별 컨테이너 수 카운트")
    @GetMapping("/containers/count-by-agent")
    // HTTP GET 메서드 매핑, URL: /api/dashboard/containers/count-by-agent
    // RESTful URL 설계: 리소스 계층 구조 표현 (containers > count-by-agent)
    public ApiResponse<List<AgentContainerCountDTO>> getContainerCountByAgent() {
        log.info("GET /api/dashboard/containers/count-by-agent - Agent별 컨테이너 개수 집계");

        // Service 계층에서 집계 로직 수행 (GROUP BY 쿼리)
        List<AgentContainerCountDTO> agentContainerCounts = dashboardService.getContainerCountByAgent();

        return ApiResponse.ok(agentContainerCounts, "Agent별 컨테이너 개수를 성공적으로 집계했습니다.");
    }


    /**
     * 네트워크 통계 시계열 데이터 조회 API
     *
     * <p><b>기능:</b></p>
     * <ul>
     *   <li>컨테이너의 네트워크 송수신 속도를 시계열 데이터로 조회</li>
     *   <li>차트 렌더링 성능을 위한 다운샘플링 지원</li>
     *   <li>시간 범위 선택 가능 (15분, 30분, 1시간)</li>
     * </ul>
     *
     * <p><b>디자인 패턴:</b></p>
     * <ul>
     *   <li>Path Variable 패턴: 리소스 식별자를 URL 경로에 포함</li>
     *   <li>Query Parameter 패턴: 조회 옵션(timeRange, detail)을 쿼리 파라미터로 전달</li>
     *   <li>다운샘플링 전략: detail 플래그로 데이터 밀도 조절 (성능 최적화)</li>
     * </ul>
     *
     * @param containerId 컨테이너 ID (URL 경로에서 추출)
     * @param timeRange 시간 범위 (FIFTEEN_MINUTES, THIRTY_MINUTES, ONE_HOUR)
     * @param detail 상세 여부 (false: 50포인트, true: 200포인트)
     * @return 네트워크 rx/tx 속도 시계열 데이터
     */
    @Operation(summary = "네트워크 통계 시계열 조회",
            description = "컨테이너의 네트워크 rx/tx 속도 시계열 데이터를 조회합니다. detail=false는 대시보드용(50포인트), detail=true는 상세보기용(200포인트)")
    @GetMapping("/containers/{containerId}/network-stats")
    // HTTP GET 메서드 매핑, URL: /api/dashboard/containers/{containerId}/network-stats
    // {containerId}는 Path Variable로 동적 URL 경로 구성
    public ApiResponse<NetworkStatsTimeSeriesDTO> getNetworkStatsTimeSeries(
            @PathVariable Long containerId,
            // @PathVariable: URL 경로에서 변수 값 추출
            // RESTful 원칙: 리소스 식별자는 URL 경로에 포함

            @Parameter(description = "시간 범위 (FIFTEEN_MINUTES, THIRTY_MINUTES, ONE_HOUR)")
            @RequestParam(defaultValue = "THIRTY_MINUTES") TimeRange timeRange,
            // defaultValue: 파라미터 미제공 시 기본값 사용

            @Parameter(description = "상세 여부 (false: 50포인트, true: 200포인트)")
            @RequestParam(defaultValue = "false") boolean detail) {
            // detail 플래그: 차트 렌더링 성능과 데이터 정밀도의 트레이드오프

        log.info("GET /api/dashboard/containers/{}/network-stats - timeRange: {}, detail: {}",
                containerId, timeRange, detail);

        // Service 계층에서 시계열 데이터 조회 및 다운샘플링 수행
        NetworkStatsTimeSeriesDTO networkStats = dashboardService.getNetworkStatsTimeSeries(
                containerId, timeRange, detail
        );

        return ApiResponse.ok(networkStats, "네트워크 통계 시계열 데이터를 성공적으로 조회했습니다.");
    }

    /**
     * Block I/O 통계 시계열 데이터 조회 API
     *
     * <p><b>기능:</b></p>
     * <ul>
     *   <li>컨테이너의 디스크 읽기/쓰기 속도를 시계열 데이터로 조회</li>
     *   <li>차트 렌더링 성능을 위한 다운샘플링 지원</li>
     *   <li>짧은 시간 범위부터 긴 시간 범위까지 유연하게 지원</li>
     * </ul>
     *
     * <p><b>사용 사례:</b></p>
     * <ul>
     *   <li>디스크 I/O 부하 모니터링</li>
     *   <li>데이터베이스 컨테이너 성능 분석</li>
     *   <li>로그 파일 쓰기 패턴 파악</li>
     * </ul>
     *
     * @param containerId 컨테이너 ID (URL 경로에서 추출)
     * @param timeRange 시간 범위 (ONE_MINUTES, THREE_MINUTES, FIFTEEN_MINUTES, THIRTY_MINUTES, ONE_HOUR)
     * @param detail 상세 여부 (false: 50포인트, true: 200포인트)
     * @return 블록 디바이스 읽기/쓰기 속도 시계열 데이터
     */
    @Operation(summary = "Block I/O 통계 시계열 조회",
            description = "컨테이너의 블록 디바이스 읽기/쓰기 속도 시계열 데이터를 조회합니다. detail=false는 대시보드용(50포인트), detail=true는 상세보기용(200포인트)")
    @GetMapping("/containers/{containerId}/blockio-stats")
    // HTTP GET 메서드 매핑, URL: /api/dashboard/containers/{containerId}/blockio-stats
    // RESTful URL 설계: 리소스 계층 구조 표현 (containers > blockio-stats)
    public ApiResponse<BlockIOStatsTimeSeriesDTO> getBlockIOStatsTimeSeries(
            @PathVariable Long containerId,
            // @PathVariable: URL 경로에서 컨테이너 ID 추출

            @Parameter(description = "시간 범위 (ONE_MINUTES, THREE_MINUTES, FIFTEEN_MINUTES, THIRTY_MINUTES, ONE_HOUR)")
            @RequestParam(defaultValue = "ONE_MINUTES") TimeRange timeRange,
            // Block I/O는 변화가 빠르므로 기본값을 1분으로 설정

            @Parameter(description = "상세 여부 (false: 50포인트, true: 200포인트)")
            @RequestParam(defaultValue = "false") boolean detail) {

        log.info("GET /api/dashboard/containers/{}/blockio-stats - timeRange: {}, detail: {}",
                containerId, timeRange, detail);

        // Service 계층에서 시계열 데이터 조회 및 다운샘플링 수행
        BlockIOStatsTimeSeriesDTO blockIOStats = dashboardService.getBlockIOStatsTimeSeries(
                containerId, timeRange, detail
        );

        return ApiResponse.ok(blockIOStats, "Block I/O 통계 시계열 데이터를 성공적으로 조회했습니다.");
    }

    /**
     * 컨테이너 상세 메트릭 조회 API
     *
     * <p><b>기능:</b></p>
     * <ul>
     *   <li>컨테이너의 모든 상세 정보를 한 번에 조회 (Single Request 최적화)</li>
     *   <li>CPU, 메모리, 네트워크, 스토리지, 로그 등 모든 메트릭 포함</li>
     *   <li>중첩된 DTO 구조로 관련 데이터를 논리적으로 그룹화</li>
     *   <li>날짜별 로그 집계 조회 지원</li>
     * </ul>
     *
     * <p><b>디자인 패턴:</b></p>
     * <ul>
     *   <li>Aggregate Root 패턴: 컨테이너를 중심으로 관련된 모든 데이터 집계</li>
     *   <li>Single Round-Trip: 여러 API 호출 대신 한 번의 요청으로 모든 데이터 반환</li>
     *   <li>날짜 포맷 자동 변환: @DateTimeFormat을 통한 ISO 8601 표준 지원</li>
     * </ul>
     *
     * <p><b>사용 사례:</b></p>
     * <ul>
     *   <li>대시보드 상세 패널 최초 로드</li>
     *   <li>컨테이너 전체 상태 스냅샷 확인</li>
     *   <li>종합적인 모니터링 데이터 수집</li>
     * </ul>
     *
     * @param containerId 컨테이너 ID (URL 경로에서 추출)
     * @param date 로그 집계 기준 날짜 (yyyy-MM-dd 형식, 생략 시 서버 시간 사용)
     * @return 중첩 구조의 컨테이너 상세 메트릭 (로그, 스토리지, 리소스 사용량 등)
     */
    @Operation(summary = "컨테이너 상세 메트릭 조회",
            description = "최초 상세 패널 로드 시 사용하는 API. 중첩 구조로 구성된 컨테이너 상세 정보(로그, 스토리지 포함)를 반환합니다. date 파라미터로 로그 집계 기준 날짜를 지정할 수 있습니다.")
    @GetMapping("/containers/{containerId}/metrics")
    // HTTP GET 메서드 매핑, URL: /api/dashboard/containers/{containerId}/metrics
    // 컨테이너의 모든 메트릭을 한 번에 조회하는 Aggregate API
    public ApiResponse<DashboardContainerDetailDTO> getContainerDetailMetrics(
            @PathVariable Long containerId,
            // @PathVariable: URL 경로에서 컨테이너 ID 추출

            @Parameter(description = "로그 집계 기준 날짜 (yyyy-MM-dd 형식, 생략 시 서버 시간 사용)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
            // @DateTimeFormat: String → LocalDate 자동 변환 (Spring 컨버터)
            // ISO.DATE: yyyy-MM-dd 형식 (ISO 8601 표준)
            // required = false: 선택적 파라미터, null 허용

        log.info("GET /api/dashboard/containers/{}/metrics - 컨테이너 상세 메트릭 조회 (date: {})", containerId, date);

        // Service 계층에서 여러 데이터 소스를 조합하여 통합 메트릭 생성
        DashboardContainerDetailDTO metrics =
                dashboardService.getContainerDetailMetrics(containerId, date);

        return ApiResponse.ok(metrics, "컨테이너 상세 메트릭을 성공적으로 조회했습니다.");
    }
}

package com.monito.domains.history.controller;

import com.monito.domains.history.dto.request.ContainerChartRequest;
import com.monito.domains.history.dto.request.ContainerHistoryRequest;
import com.monito.domains.history.dto.response.ContainerChartResponse;
import com.monito.domains.history.dto.response.ContainerHistoryPageResponse;
import com.monito.domains.history.dto.response.ContainerListForHistoryDTO;
import com.monito.domains.history.service.ContainerHistoryService;
import com.monito.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 컨테이너 히스토리 API Controller
 *
 * <p><b>역할:</b></p>
 * <ul>
 *   <li>컨테이너의 과거 메트릭 데이터 조회 API 제공</li>
 *   <li>HTTP 요청을 받아 Service 계층으로 전달하고 응답 반환 (Presentation Layer)</li>
 *   <li>RESTful API 설계 원칙 준수 (Resource-based URL, HTTP Method 활용)</li>
 * </ul>
 *
 * <p><b>제공 기능:</b></p>
 * <ul>
 *   <li>히스토리 데이터 조회 (페이지네이션, 필터링, 정렬)</li>
 *   <li>차트 데이터 조회 (특정 메트릭의 시계열 데이터, 다운샘플링)</li>
 *   <li>컨테이너 목록 조회 (히스토리 조회용 필터 지원)</li>
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
@Tag(name = "Container History", description = "컨테이너 히스토리 조회 API - 과거 메트릭 데이터 조회 및 분석")
// Swagger/OpenAPI 문서에서 API를 그룹화하고 설명 추가
// 장점: API 문서 자동 생성, 프론트엔드 개발자와 협업 용이

@Slf4j
// Lombok이 자동으로 Logger 인스턴스를 생성 (private static final Logger log = ...)
// 장점: 로깅 코드 간결화, 로그 프레임워크 교체 용이 (SLF4J 추상화)

@RestController
// @Controller + @ResponseBody의 결합
// 모든 메서드의 반환 값이 HTTP Response Body에 직접 작성됨 (JSON 직렬화)
// 장점: RESTful API 개발에 최적화, @ResponseBody를 매번 작성할 필요 없음

@RequestMapping("/api/history")
// 이 컨트롤러의 모든 엔드포인트는 /api/history로 시작
// 장점: URL 중복 제거, 일관된 API 경로 관리

@RequiredArgsConstructor
// Lombok이 final 필드에 대한 생성자를 자동 생성
// Spring의 생성자 주입 패턴 구현 (Constructor Injection)
// 장점:
// - 불변성 보장: final 필드는 한 번만 초기화 가능
// - 순환 참조 방지: 컴파일 타임에 순환 의존성 감지
// - 테스트 용이성: Mock 객체 주입 쉬움
public class HistoryController {

    /**
     * 컨테이너 히스토리 서비스
     * <p>의존성 주입 (Dependency Injection) 패턴</p>
     * <ul>
     *   <li>final 키워드로 불변성 보장</li>
     *   <li>@RequiredArgsConstructor를 통한 생성자 주입</li>
     *   <li>인터페이스 타입으로 선언하여 느슨한 결합 (Loose Coupling) 달성</li>
     * </ul>
     */
    private final ContainerHistoryService containerHistoryService;

    @Operation(
            summary = "컨테이너 히스토리 조회",
            description = """
                    컨테이너의 과거 메트릭 데이터를 기간별로 조회합니다.

                    **주요 기능:**
                    - 특정 기간 내 컨테이너 메트릭 조회 (CPU, Memory, Network, Storage 등)
                    - 삭제된 컨테이너 포함 조회 가능
                    - 페이지네이션 지원
                    - 정렬 옵션 제공

                    **날짜 형식:** yyyy-MM-dd'T'HH:mm:ss (예: 2024-01-01T00:00:00)
                    """
    )
    @GetMapping("/containers")
    public ApiResponse<ContainerHistoryPageResponse> getContainerHistory(
            @Parameter(description = "조회 시작 시간 (yyyy-MM-dd'T'HH:mm:ss 형식)", required = true, example = "2024-01-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,

            @Parameter(description = "조회 종료 시간 (yyyy-MM-dd'T'HH:mm:ss 형식)", required = true, example = "2024-01-31T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,

            @Parameter(description = "컨테이너 ID (선택, 특정 컨테이너만 조회)", example = "123")
            @RequestParam(required = false) Long containerId,

            @Parameter(description = "삭제 여부 필터 (0: 활성, 1: 삭제됨, null: 전체)", example = "0")
            @RequestParam(required = false) Integer isDeleted,

            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") Integer page,

            @Parameter(description = "페이지 크기 (한 페이지당 데이터 개수)", example = "20")
            @RequestParam(defaultValue = "20") Integer size,

            @Parameter(description = "정렬 기준 (엔티티 필드명)", example = "collectedAt")
            @RequestParam(defaultValue = "collectedAt") String sortBy,

            @Parameter(description = "정렬 방향 (ASC: 오름차순, DESC: 내림차순)", example = "DESC")
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        log.info("컨테이너 히스토리 조회 요청 - startTime: {}, endTime: {}, containerId: {}, isDeleted: {}, page: {}, size: {}",
                startTime, endTime, containerId, isDeleted, page, size);

        ContainerHistoryRequest request = new ContainerHistoryRequest(
                startTime, endTime, containerId, isDeleted,
                page, size, sortBy, sortDirection
        );

        ContainerHistoryPageResponse response = containerHistoryService.getContainerHistory(request);

        return ApiResponse.ok(response);
    }

    @Operation(
            summary = "히스토리 조회용 컨테이너 목록",
            description = """
                    히스토리 조회 시 필터링할 수 있도록 컨테이너 목록을 제공합니다.

                    **주요 기능:**
                    - 활성 컨테이너와 삭제된 컨테이너 목록 조회
                    - 컨테이너 ID, 이름, 해시 정보 제공
                    - 삭제 여부 필터링 지원

                    **반환 필드:**
                    - id: 컨테이너 ID
                    - containerName: 컨테이너 이름
                    - containerHash: 컨테이너 해시 (12자리)
                    - isDeleted: 삭제 여부 (0: 활성, 1: 삭제됨)
                    """
    )
    @GetMapping("/containers/list")
    public ApiResponse<List<ContainerListForHistoryDTO>> getContainerListForHistory(
            @Parameter(
                    description = """
                            삭제 여부 필터:
                            - 0: 활성 컨테이너만 조회
                            - 1: 삭제된 컨테이너만 조회
                            - null: 전체 조회 (활성 + 삭제됨)
                            """,
                    example = "0"
            )
            @RequestParam(required = false) Integer isDeleted
    ) {
        log.info("히스토리 조회용 컨테이너 목록 요청 - isDeleted: {}", isDeleted);

        List<ContainerListForHistoryDTO> containers = containerHistoryService.getContainerListForHistory(isDeleted);

        return ApiResponse.ok(containers, "컨테이너 목록을 성공적으로 조회했습니다.");
    }

    /**
     * 컨테이너 차트 데이터 조회 API
     *
     * <p><b>기능:</b></p>
     * <ul>
     *   <li>특정 컨테이너의 특정 메트릭을 시계열 차트 데이터로 조회</li>
     *   <li>TimeSeriesDownSampler를 통해 자동 다운샘플링 적용 (약 60개 포인트)</li>
     *   <li>프론트엔드 차트 라이브러리에서 바로 사용 가능한 형식으로 반환</li>
     * </ul>
     *
     * <p><b>디자인 패턴:</b></p>
     * <ul>
     *   <li>Query Parameter 패턴: 조회 조건을 URL 쿼리 파라미터로 전달</li>
     *   <li>동적 필드 선택: metricField 파라미터로 40개 이상의 메트릭 지원</li>
     *   <li>통일된 응답 형식: ApiResponse로 감싸서 일관성 있는 API 제공</li>
     * </ul>
     *
     * @param startTime 조회 시작 시간 (ISO 8601 형식: yyyy-MM-dd'T'HH:mm:ss)
     * @param endTime 조회 종료 시간
     * @param containerId 컨테이너 ID
     * @param metricField 조회할 메트릭 필드명 (예: cpuPercent, memPercent)
     * @return 시계열 차트 데이터 (다운샘플링 적용)
     */
    @Operation(
            summary = "컨테이너 차트 데이터 조회",
            description = """
                    컨테이너의 특정 메트릭 필드를 시계열 데이터로 조회합니다.

                    **주요 기능:**
                    - ContainerHistoryResponse의 특정 필드만 선택하여 조회
                    - 시계열 데이터 형태로 반환 (timestamp, value)
                    - 차트 렌더링에 최적화된 데이터 구조

                    **지원 메트릭 필드:**
                    - **CPU**: cpuPercent, cpuCoreUsage, cpuUsageTotal 등
                    - **Memory**: memPercent, memUsage, memMaxUsage 등
                    - **Network**: rxBytes, txBytes, rxBytesPerSec, txBytesPerSec 등
                    - **Block I/O**: blkRead, blkWrite, blkReadPerSec, blkWritePerSec 등
                    - **Storage**: sizeRw, sizeRootFs 등

                    **날짜 형식:** yyyy-MM-dd'T'HH:mm:ss (예: 2024-01-01T00:00:00)
                    """
    )
    @GetMapping("/containers/chart")
    // HTTP GET 메서드 매핑, URL: /api/history/containers/chart
    // RESTful 원칙: GET은 데이터 조회(멱등성), 상태 변경 없음
    public ApiResponse<ContainerChartResponse> getContainerChart(
            @Parameter(description = "조회 시작 시간 (yyyy-MM-dd'T'HH:mm:ss 형식)", required = true, example = "2024-01-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,

            @Parameter(description = "조회 종료 시간 (yyyy-MM-dd'T'HH:mm:ss 형식)", required = true, example = "2024-01-31T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,

            @Parameter(description = "컨테이너 ID", required = true, example = "123")
            @RequestParam Long containerId,

            @Parameter(description = """
                    메트릭 필드명 (ContainerHistoryResponse의 필드명)

                    예시:
                    - cpuPercent: CPU 사용률
                    - memPercent: 메모리 사용률
                    - rxBytes: 네트워크 수신 바이트
                    - txBytes: 네트워크 송신 바이트
                    - blkRead: 블록 읽기 바이트
                    - blkWrite: 블록 쓰기 바이트
                    """, required = true, example = "cpuPercent")
            @RequestParam String metricField
    ) {
        log.info("컨테이너 차트 데이터 조회 요청 - startTime: {}, endTime: {}, containerId: {}, metricField: {}",
                startTime, endTime, containerId, metricField);

        ContainerChartRequest request = new ContainerChartRequest(
                startTime, endTime, containerId, metricField
        );

        ContainerChartResponse response = containerHistoryService.getContainerChart(request);

        return ApiResponse.ok(response, "차트 데이터를 성공적으로 조회했습니다.");
    }
}

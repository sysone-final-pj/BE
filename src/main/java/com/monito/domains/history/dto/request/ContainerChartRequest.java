package com.monito.domains.history.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 컨테이너 차트 데이터 조회 요청 DTO (Data Transfer Object)
 *
 * <p><b>목적:</b></p>
 * <ul>
 *   <li>ContainerHistoryResponse의 특정 메트릭 필드를 시계열 차트 데이터로 조회</li>
 *   <li>프론트엔드에서 선택한 메트릭의 시간별 추이를 그래프로 표시하기 위한 데이터 요청</li>
 *   <li>다운샘플링을 통해 성능 최적화된 차트 데이터 제공</li>
 * </ul>
 *
 * <p><b>DTO 패턴의 장점:</b></p>
 * <ul>
 *   <li>계층 간 데이터 전송 객체로 사용하여 관심사 분리 (Separation of Concerns)</li>
 *   <li>엔티티(Entity)와 분리하여 도메인 로직 보호</li>
 *   <li>API 스펙 변경에 유연하게 대응 (엔티티 변경 없이 DTO만 수정 가능)</li>
 *   <li>불필요한 정보 노출 방지 (필요한 필드만 포함)</li>
 *   <li>유효성 검증 로직을 DTO에 집중시켜 책임 분산</li>
 * </ul>
 *
 * <p><b>사용 예시:</b></p>
 * <pre>
 * GET /api/history/containers/chart?
 *   startTime=2024-01-01T00:00:00&
 *   endTime=2024-01-31T23:59:59&
 *   containerId=123&
 *   metricField=cpuPercent
 * </pre>
 */
@Schema(description = "컨테이너 차트 데이터 조회 요청")
// ========== Lombok 어노테이션 ==========
// Lombok: 반복적인 보일러플레이트 코드를 자동 생성하여 코드 간결성 향상

@Getter
// Lombok이 모든 필드에 대한 getter 메서드를 자동 생성
// 장점:
// - 수동으로 getter 작성 불필요 (코드 간결성)
// - 필드 추가/삭제 시 getter도 자동으로 추가/삭제 (유지보수성)
// - 일관된 네이밍 규칙 보장 (getFieldName() 형식)

@NoArgsConstructor
// 파라미터가 없는 기본 생성자를 자동 생성
// 장점:
// - Jackson/JSON 역직렬화에 필수 (객체 생성 후 setter로 값 주입)
// - JPA 엔티티는 기본 생성자가 필수 (Proxy 생성 시 사용)
// - 리플렉션(Reflection) 기반 프레임워크 호환성

@AllArgsConstructor
// 모든 필드를 파라미터로 받는 생성자를 자동 생성
// 장점:
// - 불변 객체(Immutable Object) 생성 가능
// - 객체 생성 시 모든 필수 값을 한 번에 초기화 (일관성 보장)
// - 테스트 코드 작성 시 편리함
public class ContainerChartRequest {

    // ========== 요청 파라미터 ==========

    /**
     * 조회 시작 시간
     * <p>@Schema: Swagger/OpenAPI 문서 자동 생성을 위한 메타데이터</p>
     * <ul>
     *   <li>API 문서에 필드 설명, 예시, 필수 여부 등을 자동으로 표시</li>
     *   <li>프론트엔드 개발자가 API 스펙을 쉽게 이해 가능</li>
     *   <li>Swagger UI에서 API 테스트 시 자동 완성 지원</li>
     * </ul>
     */
    @Schema(description = "조회 시작 시간", example = "2024-01-01T00:00:00", required = true)
    private LocalDateTime startTime;

    /**
     * 조회 종료 시간
     * <p>시간 범위 조회 패턴을 통해 특정 기간의 데이터만 효율적으로 조회</p>
     */
    @Schema(description = "조회 종료 시간", example = "2024-01-31T23:59:59", required = true)
    private LocalDateTime endTime;

    /**
     * 컨테이너 ID
     * <p>특정 컨테이너의 메트릭만 필터링하여 조회</p>
     */
    @Schema(description = "컨테이너 ID", example = "123", required = true)
    private Long containerId;

    /**
     * 조회할 메트릭 필드명
     * <p>동적 필드 선택 패턴을 통해 다양한 메트릭을 하나의 API로 제공</p>
     * <ul>
     *   <li>장점: 메트릭별로 API를 따로 만들 필요 없음 (코드 재사용성)</li>
     *   <li>프론트엔드에서 원하는 메트릭을 자유롭게 선택 가능</li>
     *   <li>새로운 메트릭 추가 시 API 엔드포인트 변경 불필요</li>
     * </ul>
     */
    @Schema(description = """
            조회할 메트릭 필드명 (ContainerHistoryResponse의 필드명)

            **CPU 메트릭:**
            - cpuPercent: CPU 사용률
            - cpuCoreUsage: CPU 코어 사용량
            - cpuUsageTotal: 전체 CPU 사용 시간

            **Memory 메트릭:**
            - memPercent: 메모리 사용률
            - memUsage: 메모리 사용량 (bytes)
            - memMaxUsage: 최대 메모리 사용량 (bytes)

            **Network 메트릭:**
            - rxBytes: 네트워크 수신 바이트
            - txBytes: 네트워크 송신 바이트
            - rxBytesPerSec: 초당 수신 바이트
            - txBytesPerSec: 초당 송신 바이트
            - networkTotalBytes: 전체 네트워크 사용량

            **Block I/O 메트릭:**
            - blkRead: 블록 읽기 바이트
            - blkWrite: 블록 쓰기 바이트
            - blkReadPerSec: 초당 블록 읽기
            - blkWritePerSec: 초당 블록 쓰기

            **Storage 메트릭:**
            - sizeRw: R/W 레이어 크기
            - sizeRootFs: 루트 파일시스템 크기
            """,
            example = "cpuPercent", required = true)
    private String metricField;
}
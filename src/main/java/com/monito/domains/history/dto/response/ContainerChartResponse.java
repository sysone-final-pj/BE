package com.monito.domains.history.dto.response;

import com.monito.domains.container.dto.response.metrics.TimeSeriesDataDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 컨테이너 차트 데이터 응답 DTO (Data Transfer Object)
 *
 * <p><b>목적:</b></p>
 * <ul>
 *   <li>특정 메트릭 필드의 시계열 데이터를 차트 렌더링에 최적화된 형태로 반환</li>
 *   <li>TimeSeriesDownSampler를 통해 다운샘플링된 데이터 제공 (성능 최적화)</li>
 *   <li>원본 데이터 개수와 샘플링된 데이터 개수를 함께 제공하여 투명성 보장</li>
 * </ul>
 *
 * <p><b>응답 예시:</b></p>
 * <pre>
 * {
 *   "containerId": 123,
 *   "metricField": "cpuPercent",
 *   "dataPoints": [
 *     {"timestamp": "2024-01-01T00:00:00", "value": 45.5},
 *     {"timestamp": "2024-01-01T00:15:00", "value": 50.2}
 *   ],
 *   "originalCount": 1440,
 *   "sampledCount": 60
 * }
 * </pre>
 *
 * <p><b>다운샘플링 패턴:</b></p>
 * <ul>
 *   <li>대량의 데이터 포인트를 약 60개로 압축하여 전송 (네트워크 비용 절감)</li>
 *   <li>클라이언트의 렌더링 부하 감소 (브라우저 성능 향상)</li>
 *   <li>평균값 기반 집계로 데이터 손실 최소화</li>
 * </ul>
 */
@Schema(description = "컨테이너 차트 데이터 응답 - 시계열 데이터 목록")
// ========== Lombok 어노테이션 ==========

@Getter
// Lombok이 모든 필드에 대한 getter 메서드를 자동 생성

@Builder
// Builder 패턴을 자동으로 구현
// 장점:
// - 가독성 향상: 필드가 많을 때 명확한 객체 생성 가능
//   예: ContainerChartResponse.builder()
//          .containerId(123L)
//          .metricField("cpuPercent")
//          .dataPoints(points)
//          .build();
// - 불변 객체 생성 용이: setter 없이 객체 생성 가능
// - 선택적 파라미터 지원: 필요한 필드만 선택하여 설정 가능
// - 메서드 체이닝(Method Chaining)으로 코드 간결성 향상

@NoArgsConstructor
// Jackson 역직렬화를 위한 기본 생성자 (JSON → Java 객체 변환 시 필요)

@AllArgsConstructor
// @Builder와 함께 사용하여 모든 필드를 초기화하는 생성자 제공
public class ContainerChartResponse {

    // ========== 응답 필드 ==========

    /**
     * 컨테이너 ID
     * <p>어떤 컨테이너의 데이터인지 식별</p>
     */
    @Schema(description = "컨테이너 ID", example = "123")
    private Long containerId;

    /**
     * 메트릭 필드명
     * <p>어떤 메트릭의 데이터인지 명시 (예: cpuPercent, memPercent)</p>
     */
    @Schema(description = "메트릭 필드명", example = "cpuPercent")
    private String metricField;

    /**
     * 시계열 데이터 포인트 목록
     * <p>TimeSeriesDataDTO: timestamp(시간)과 value(값)를 포함하는 데이터 포인트</p>
     * <ul>
     *   <li>다운샘플링된 데이터로 약 60개 포인트 포함</li>
     *   <li>차트 라이브러리에서 바로 사용 가능한 형식</li>
     * </ul>
     */
    @Schema(description = "시계열 데이터 포인트 목록 (timestamp, value)")
    private List<TimeSeriesDataDTO> dataPoints;

    /**
     * 다운샘플링 전 원본 데이터 포인트 개수
     * <p>얼마나 많은 데이터가 압축되었는지 확인 가능 (투명성)</p>
     */
    @Schema(description = "다운샘플링 전 원본 데이터 포인트 개수", example = "1000")
    private Integer originalCount;

    /**
     * 다운샘플링 후 반환된 데이터 포인트 개수
     * <p>실제로 전송된 데이터 포인트 개수 (대부분 60개 내외)</p>
     */
    @Schema(description = "다운샘플링 후 반환된 데이터 포인트 개수", example = "60")
    private Integer sampledCount;
}
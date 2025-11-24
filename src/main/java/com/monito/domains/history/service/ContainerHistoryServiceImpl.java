package com.monito.domains.history.service;

import com.monito.domains.container.domain.ContainerHealth;
import com.monito.domains.container.domain.ContainerState;
import com.monito.domains.container.dto.response.metrics.TimeSeriesDataDTO;
import com.monito.domains.container.repository.ContainerRepository;
import com.monito.domains.container.util.TimeSeriesDownSampler;
import com.monito.domains.history.dto.request.ContainerChartRequest;
import com.monito.domains.history.dto.request.ContainerHistoryRequest;
import com.monito.domains.history.dto.response.ContainerChartResponse;
import com.monito.domains.history.dto.response.ContainerHistoryPageResponse;
import com.monito.domains.history.dto.response.ContainerHistoryResponse;
import com.monito.domains.history.dto.response.ContainerListForHistoryDTO;
import com.monito.domains.history.repository.ContainerHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 컨테이너 히스토리 서비스 구현체
 *
 * <p><b>역할:</b></p>
 * <ul>
 *   <li>비즈니스 로직 처리 (Business Logic Layer)</li>
 *   <li>Repository를 통한 데이터 조회 및 가공</li>
 *   <li>DTO 변환 및 다운샘플링 적용</li>
 * </ul>
 *
 * <p><b>주요 패턴:</b></p>
 * <ul>
 *   <li>Service 패턴: 비즈니스 로직을 캡슐화하여 재사용성 향상</li>
 *   <li>DTO 변환 패턴: Entity와 DTO 분리로 계층 간 결합도 감소</li>
 *   <li>읽기 전용 트랜잭션: @Transactional(readOnly = true)로 성능 최적화</li>
 * </ul>
 */
@Slf4j
// Lombok: Logger 인스턴스 자동 생성

@Service
// Spring의 서비스 컴포넌트로 등록
// @Component의 특수화된 형태로, 비즈니스 로직 계층임을 명시
// 장점: 컴포넌트 스캔으로 자동 빈 등록, 명확한 계층 구분

@RequiredArgsConstructor
// Lombok: final 필드에 대한 생성자 자동 생성 (생성자 주입 패턴)

@Transactional(readOnly = true)
// 클래스 레벨 트랜잭션 설정: 모든 메서드를 읽기 전용 트랜잭션으로 실행
// 장점:
// - 읽기 전용 최적화: Hibernate가 변경 감지(Dirty Checking)를 수행하지 않음
// - 데이터베이스 성능 향상: DB는 읽기 전용 모드로 최적화 가능
// - 실수로 데이터 변경 방지: 읽기만 수행해야 하는 메서드의 안전성 보장
// - 쓰기가 필요한 메서드는 @Transactional(readOnly = false) 또는 @Transactional로 오버라이드
public class ContainerHistoryServiceImpl implements ContainerHistoryService {

    /**
     * 컨테이너 히스토리 데이터 저장소
     * <p>히스토리 데이터(ContainerStatsLog) 조회를 담당</p>
     */
    private final ContainerHistoryRepository containerHistoryRepository;

    /**
     * 컨테이너 정보 저장소
     * <p>컨테이너 목록 조회를 담당</p>
     */
    private final ContainerRepository containerRepository;

    @Override
    public ContainerHistoryPageResponse getContainerHistory(ContainerHistoryRequest request) {
        // 엔티티 필드명을 데이터베이스 컬럼명으로 변환(네이티브쿼리)
        String sortColumn = convertToColumnName(request.getSortBy());

        // 페이지 요청 생성 (정렬 포함)
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                Sort.by(
                        "DESC".equalsIgnoreCase(request.getSortDirection())
                                ? Sort.Direction.DESC
                                : Sort.Direction.ASC,
                        sortColumn
                )
        );

        // Repository에서 데이터 조회
        Page<Object[]> resultPage = containerHistoryRepository.findContainerHistory(
                request.getStartTime(),
                request.getEndTime(),
                request.getContainerId(),
                request.getIsDeleted(),
                pageable
        );

        // Object[] -> ContainerHistoryResponse 변환
        List<ContainerHistoryResponse> content = resultPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        // 페이지 응답 생성
        return ContainerHistoryPageResponse.builder()
                .content(content)
                .pageNumber(resultPage.getNumber())
                .pageSize(resultPage.getSize())
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .first(resultPage.isFirst())
                .last(resultPage.isLast())
                .hasNext(resultPage.hasNext())
                .hasPrevious(resultPage.hasPrevious())
                .build();
    }

    /**
     * Object[] 배열을 ContainerHistoryResponse로 변환
     */
    private ContainerHistoryResponse mapToResponse(Object[] row) {
        int idx = 0;

        return ContainerHistoryResponse.builder()
                // 기본 정보
                .collectedAt(toLocalDateTime(row[idx++]))
                .containerName((String) row[idx++])
                .containerHash((String) row[idx++])
                .agentName((String) row[idx++])
                .imgNameTag((String) row[idx++])
                .state(ContainerState.valueOf((String) row[idx++]))
                .health(ContainerHealth.valueOf((String) row[idx++]))
                .containerCreatedAt(toLocalDateTime(row[idx++]))
                .isDeleted(((Number) row[idx++]).intValue())

                // CPU 메트릭
                .cpuPercent(toBigDecimal(row[idx++]))
                .cpuCoreUsage(toBigDecimal(row[idx++]))
                .hostCpuUsageTotal(toLong(row[idx++]))
                .cpuUsageTotal(toLong(row[idx++]))
                .cpuUser(toLong(row[idx++]))
                .cpuSystem(toLong(row[idx++]))
                .cpuQuota(toLong(row[idx++]))
                .cpuPeriod(toLong(row[idx++]))
                .onlineCpus(toInteger(row[idx++]))
                .throttlingPeriods(toLong(row[idx++]))
                .throttledPeriods(toLong(row[idx++]))
                .throttledTime(toLong(row[idx++]))
                .cpuLimitCores(toBigDecimal(row[idx++]))
                .isCpuUnlimited(toBoolean(row[idx++]))

                // Memory 메트릭
                .memPercent(toBigDecimal(row[idx++]))
                .memUsage(toLong(row[idx++]))
                .memMaxUsage(toLong(row[idx++]))
                .memLimit(toLong(row[idx++]))
                .isMemoryUnlimited(toBoolean(row[idx++]))
                .lastOomKilledAt(toLocalDateTime(row[idx++]))

                // Block I/O 메트릭
                .blkRead(toLong(row[idx++]))
                .blkWrite(toLong(row[idx++]))
                .blkReadPerSec(toLong(row[idx++]))
                .blkWritePerSec(toLong(row[idx++]))

                // Network 메트릭
                .rxBytes(toLong(row[idx++]))
                .txBytes(toLong(row[idx++]))
                .rxPackets(toLong(row[idx++]))
                .txPackets(toLong(row[idx++]))
                .networkTotalBytes(toLong(row[idx++]))
                .rxBytesPerSec(toLong(row[idx++]))
                .txBytesPerSec(toLong(row[idx++]))
                .rxPps(toLong(row[idx++]))
                .txPps(toLong(row[idx++]))
                .rxFailureRate(toBigDecimal(row[idx++]))
                .txFailureRate(toBigDecimal(row[idx++]))
                .rxErrors(toInteger(row[idx++]))
                .txErrors(toInteger(row[idx++]))
                .rxDropped(toInteger(row[idx++]))
                .txDropped(toInteger(row[idx++]))

                // Storage 메트릭
                .sizeRw(toLong(row[idx++]))
                .sizeRootFs(toLong(row[idx++]))
                .storageLimit(toLong(row[idx++]))
                .isStorageUnlimited(toBoolean(row[idx++]))

                .build();
    }

    @Override
    public List<ContainerListForHistoryDTO> getContainerListForHistory(Integer isDeleted) {
        log.info("히스토리 조회용 컨테이너 목록 조회 - isDeleted: {}", isDeleted);

        // Repository에서 네이티브 쿼리로 조회
        List<Object[]> resultList = containerRepository.findContainerListForHistory(isDeleted);

        // Object[] -> ContainerListForHistoryDTO 변환
        return resultList.stream()
                .map(row -> ContainerListForHistoryDTO.builder()
                        .id(((Number) row[0]).longValue())
                        .containerName((String) row[1])
                        .containerHash((String) row[2])
                        .isDeleted(((Number) row[3]).intValue())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 컨테이너 차트 데이터 조회
     *
     * <p><b>처리 흐름:</b></p>
     * <ol>
     *   <li>Repository에서 시간 범위 내의 모든 히스토리 데이터 조회</li>
     *   <li>Object[] 배열을 ContainerHistoryResponse DTO로 변환</li>
     *   <li>요청받은 metricField만 추출하여 TimeSeriesDataDTO 리스트 생성</li>
     *   <li>TimeSeriesDownSampler를 통해 자동 다운샘플링 적용 (약 60개 포인트)</li>
     *   <li>ContainerChartResponse로 최종 응답 생성</li>
     * </ol>
     *
     * <p><b>핵심 패턴:</b></p>
     * <ul>
     *   <li><b>동적 필드 추출:</b> extractMetricValue() 메서드에서 switch 문으로 40개 이상의 필드 지원</li>
     *   <li><b>다운샘플링:</b> TimeSeriesDownSampler 유틸리티 재사용 (DRY 원칙)</li>
     *   <li><b>스트림 API:</b> Java 8+ 함수형 프로그래밍으로 데이터 변환 처리</li>
     * </ul>
     *
     * @param request 차트 데이터 조회 요청 (시간 범위, 컨테이너 ID, 메트릭 필드)
     * @return 다운샘플링된 시계열 차트 데이터
     */
    @Override
    public ContainerChartResponse getContainerChart(ContainerChartRequest request) {
        log.info("컨테이너 차트 데이터 조회 - containerId: {}, metricField: {}, startTime: {}, endTime: {}",
                request.getContainerId(), request.getMetricField(), request.getStartTime(), request.getEndTime());

        // 1. 모든 히스토리 데이터 조회 (페이지 크기를 충분히 크게 설정)
        // Pageable: Spring Data JPA의 페이지네이션 인터페이스
        // - Integer.MAX_VALUE: 모든 데이터를 한 번에 조회 (다운샘플링에서 필터링)
        // - Sort.by(): 시간순 정렬 (ASC: 과거 → 현재 순서)
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.ASC, "collected_at"));
        Page<Object[]> resultPage = containerHistoryRepository.findContainerHistory(
                request.getStartTime(),
                request.getEndTime(),
                request.getContainerId(),
                null, // isDeleted 필터 사용 안 함 (모든 데이터 조회)
                pageable
        );

        // 2. 히스토리 데이터를 ContainerHistoryResponse로 변환
        // Stream API: 선언형 프로그래밍으로 데이터 변환 (가독성 향상)
        // mapToResponse(): Object[] → ContainerHistoryResponse 변환 메서드
        List<ContainerHistoryResponse> historyResponses = resultPage.getContent().stream()
                .map(this::mapToResponse) // 메서드 레퍼런스 사용
                .collect(Collectors.toList());

        // 3. 특정 메트릭 필드만 추출하여 TimeSeriesDataDTO 리스트 생성
        // 동적 필드 선택 패턴: 사용자가 요청한 metricField만 추출
        List<TimeSeriesDataDTO> timeSeriesData = new ArrayList<>();
        for (ContainerHistoryResponse response : historyResponses) {
            BigDecimal value = extractMetricValue(response, request.getMetricField());
            if (value != null) { // null 값은 제외 (누락 데이터 처리)
                timeSeriesData.add(TimeSeriesDataDTO.from(response.getCollectedAt(), value));
            }
        }

        int originalCount = timeSeriesData.size();
        log.info("원본 데이터 포인트 개수: {}", originalCount);

        // 4. 다운샘플링 적용
        // TimeSeriesDownSampler: 대량의 데이터를 약 60개로 압축 (성능 최적화)
        // - 평균값 기반 집계로 데이터 손실 최소화
        // - 네트워크 전송량 감소, 클라이언트 렌더링 부하 감소
        Duration duration = Duration.between(request.getStartTime(), request.getEndTime());
        long totalMinutes = duration.toMinutes();
        List<TimeSeriesDataDTO> sampledData = TimeSeriesDownSampler.autoDownSample(timeSeriesData, totalMinutes);

        log.info("다운샘플링 후 데이터 포인트 개수: {}", sampledData.size());

        // 5. 응답 생성
        // Builder 패턴: 가독성 높은 객체 생성
        return ContainerChartResponse.builder()
                .containerId(request.getContainerId())
                .metricField(request.getMetricField())
                .dataPoints(sampledData)
                .originalCount(originalCount)
                .sampledCount(sampledData.size())
                .build();
    }

    /**
     * ContainerHistoryResponse에서 특정 메트릭 필드의 값을 추출
     * @param response 히스토리 응답 객체
     * @param metricField 추출할 메트릭 필드명
     * @return 메트릭 값 (BigDecimal)
     */
    private BigDecimal extractMetricValue(ContainerHistoryResponse response, String metricField) {
        return switch (metricField) {
            // CPU 메트릭
            case "cpuPercent" -> response.getCpuPercent();
            case "cpuCoreUsage" -> response.getCpuCoreUsage();
            case "hostCpuUsageTotal" -> convertToBigDecimal(response.getHostCpuUsageTotal());
            case "cpuUsageTotal" -> convertToBigDecimal(response.getCpuUsageTotal());
            case "cpuUser" -> convertToBigDecimal(response.getCpuUser());
            case "cpuSystem" -> convertToBigDecimal(response.getCpuSystem());
            case "cpuQuota" -> convertToBigDecimal(response.getCpuQuota());
            case "cpuPeriod" -> convertToBigDecimal(response.getCpuPeriod());
            case "onlineCpus" -> convertToBigDecimal(response.getOnlineCpus());
            case "throttlingPeriods" -> convertToBigDecimal(response.getThrottlingPeriods());
            case "throttledPeriods" -> convertToBigDecimal(response.getThrottledPeriods());
            case "throttledTime" -> convertToBigDecimal(response.getThrottledTime());
            case "cpuLimitCores" -> response.getCpuLimitCores();

            // Memory 메트릭
            case "memPercent" -> response.getMemPercent();
            case "memUsage" -> convertToBigDecimal(response.getMemUsage());
            case "memMaxUsage" -> convertToBigDecimal(response.getMemMaxUsage());
            case "memLimit" -> convertToBigDecimal(response.getMemLimit());

            // Block I/O 메트릭
            case "blkRead" -> convertToBigDecimal(response.getBlkRead());
            case "blkWrite" -> convertToBigDecimal(response.getBlkWrite());
            case "blkReadPerSec" -> convertToBigDecimal(response.getBlkReadPerSec());
            case "blkWritePerSec" -> convertToBigDecimal(response.getBlkWritePerSec());

            // Network 메트릭
            case "rxBytes" -> convertToBigDecimal(response.getRxBytes());
            case "txBytes" -> convertToBigDecimal(response.getTxBytes());
            case "rxPackets" -> convertToBigDecimal(response.getRxPackets());
            case "txPackets" -> convertToBigDecimal(response.getTxPackets());
            case "networkTotalBytes" -> convertToBigDecimal(response.getNetworkTotalBytes());
            case "rxBytesPerSec" -> convertToBigDecimal(response.getRxBytesPerSec());
            case "txBytesPerSec" -> convertToBigDecimal(response.getTxBytesPerSec());
            case "rxPps" -> convertToBigDecimal(response.getRxPps());
            case "txPps" -> convertToBigDecimal(response.getTxPps());
            case "rxFailureRate" -> response.getRxFailureRate();
            case "txFailureRate" -> response.getTxFailureRate();
            case "rxErrors" -> convertToBigDecimal(response.getRxErrors());
            case "txErrors" -> convertToBigDecimal(response.getTxErrors());
            case "rxDropped" -> convertToBigDecimal(response.getRxDropped());
            case "txDropped" -> convertToBigDecimal(response.getTxDropped());

            // Storage 메트릭
            case "sizeRw" -> convertToBigDecimal(response.getSizeRw());
            case "sizeRootFs" -> convertToBigDecimal(response.getSizeRootFs());
            case "storageLimit" -> convertToBigDecimal(response.getStorageLimit());

            default -> throw new IllegalArgumentException("지원하지 않는 메트릭 필드입니다: " + metricField);
        };
    }

    /**
     * Number 타입을 BigDecimal로 변환 (차트 데이터용)
     */
    private BigDecimal convertToBigDecimal(Number value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return BigDecimal.valueOf(value.doubleValue());
    }

    /**
     * 엔티티 필드명을 데이터베이스 컬럼명으로 변환 (네이티브 쿼리용)
     * 카멜 케이스 -> 스네이크 케이스 변환
     * @param fieldName 엔티티 필드명
     * @return 데이터베이스 컬럼명
     */
    private String convertToColumnName(String fieldName) {
        // 카멜 케이스를 스네이크 케이스로 변환
        return fieldName.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    /**
     * Object를 LocalDateTime으로 변환 (네이티브 쿼리용)
     * - 네이티브 쿼리에서는 Oracle이 날짜/시간을 java.sql.Timestamp로 반환
     * @param obj Timestamp 또는 LocalDateTime 객체
     * @return LocalDateTime (null 가능)
     */
    private LocalDateTime toLocalDateTime(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) obj).toLocalDateTime();
        }
        if (obj instanceof LocalDateTime) {
            return (LocalDateTime) obj;
        }
        throw new IllegalArgumentException("Cannot convert " + obj.getClass() + " to LocalDateTime");
    }

    /**
     * Object를 Long으로 변환 (네이티브 쿼리용)
     * - Oracle 네이티브 쿼리에서는 숫자를 BigDecimal로 반환
     * @param obj Number 객체
     * @return Long (null 가능)
     */
    private Long toLong(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        throw new IllegalArgumentException("Cannot convert " + obj.getClass() + " to Long");
    }

    /**
     * Object를 Integer로 변환 (네이티브 쿼리용)
     * - Oracle 네이티브 쿼리에서는 숫자를 BigDecimal로 반환
     * @param obj Number 객체
     * @return Integer (null 가능)
     */
    private Integer toInteger(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        throw new IllegalArgumentException("Cannot convert " + obj.getClass() + " to Integer");
    }

    /**
     * Object를 BigDecimal로 변환 (네이티브 쿼리용)
     * @param obj BigDecimal 또는 Number 객체
     * @return BigDecimal (null 가능)
     */
    private BigDecimal toBigDecimal(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof BigDecimal) {
            return (BigDecimal) obj;
        }
        if (obj instanceof Number) {
            return BigDecimal.valueOf(((Number) obj).doubleValue());
        }
        throw new IllegalArgumentException("Cannot convert " + obj.getClass() + " to BigDecimal");
    }

    /**
     * Object를 Boolean으로 변환 (네이티브 쿼리용)
     * - Oracle에서는 Boolean을 숫자(0 또는 1)로 저장
     * @param obj Boolean, Number, 또는 String 객체
     * @return Boolean (null 가능)
     */
    private Boolean toBoolean(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        if (obj instanceof Number) {
            return ((Number) obj).intValue() != 0;
        }
        if (obj instanceof String) {
            return "1".equals(obj) || "true".equalsIgnoreCase((String) obj);
        }
        throw new IllegalArgumentException("Cannot convert " + obj.getClass() + " to Boolean");
    }
}
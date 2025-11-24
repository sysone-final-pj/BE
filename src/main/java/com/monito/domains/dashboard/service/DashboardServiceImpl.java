package com.monito.domains.dashboard.service;

import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.ContainerStatsLog;
import com.monito.domains.container.domain.LogSource;
import com.monito.domains.container.repository.ContainerLogRepository;
import com.monito.domains.container.repository.ContainerRepository;
import com.monito.domains.container.repository.ContainerStatsLogRepository;
import com.monito.domains.dashboard.dto.request.ContainerFilterDTO;
import com.monito.domains.dashboard.dto.request.ContainerSortType;
import com.monito.domains.dashboard.dto.request.TimeRange;
import com.monito.domains.dashboard.dto.response.*;
import com.monito.domains.dashboard.dto.response.metrics.*;
import com.monito.domains.dashboard.repository.DashboardRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.monito.global.exception.ExceptionMessage;
import com.monito.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대시보드 서비스 구현체
 *
 * <p><b>역할:</b></p>
 * <ul>
 *   <li>대시보드 화면에 필요한 비즈니스 로직 처리 (Business Logic Layer)</li>
 *   <li>컨테이너 목록 조회, 정렬, 필터링 로직 제공</li>
 *   <li>실시간 메트릭 데이터 조회 및 시계열 데이터 샘플링 처리</li>
 *   <li>Repository를 통한 데이터 조회 및 DTO 변환</li>
 * </ul>
 *
 * <p><b>주요 패턴:</b></p>
 * <ul>
 *   <li><b>Service 패턴:</b> 비즈니스 로직을 캡슐화하여 Controller와 Repository 사이의 중간 계층 역할</li>
 *   <li><b>DTO 변환 패턴:</b> Entity와 DTO를 분리하여 계층 간 결합도 감소, API 응답 구조 최적화</li>
 *   <li><b>읽기 전용 트랜잭션:</b> 클래스 레벨의 @Transactional(readOnly = true)로 조회 성능 최적화</li>
 *   <li><b>샘플링 패턴:</b> 대량의 시계열 데이터를 다운샘플링하여 네트워크 전송량 최적화</li>
 *   <li><b>조건부 쿼리 패턴:</b> 필터 활성화 여부에 따라 동적으로 쿼리 선택</li>
 * </ul>
 *
 * <p><b>목적:</b></p>
 * <ul>
 *   <li>대시보드에서 컨테이너 모니터링을 위한 다양한 데이터 제공</li>
 *   <li>CPU, 메모리, 네트워크, Block I/O 등의 실시간 메트릭 조회</li>
 *   <li>로그 집계, 스토리지 사용량, Agent별 컨테이너 개수 등 통계 데이터 제공</li>
 * </ul>
 */
@Slf4j
// Lombok: Logger 인스턴스 자동 생성 (log.info, log.debug, log.error 등 사용 가능)
// 장점: 보일러플레이트 코드 제거, 클래스 이름 기반 로거 자동 생성

@Service
// Spring의 서비스 컴포넌트로 등록
// @Component의 특수화된 형태로, 비즈니스 로직 계층임을 명시
// 장점:
// - 컴포넌트 스캔으로 자동 빈 등록 (Spring Context에 관리됨)
// - 명확한 계층 구분 (Controller, Service, Repository)
// - AOP 적용 가능 (트랜잭션, 로깅 등)

@RequiredArgsConstructor
// Lombok: final 필드에 대한 생성자 자동 생성 (생성자 주입 패턴)
// 장점:
// - 생성자 주입 패턴 구현 (불변성 보장, 테스트 용이)
// - 필드 추가 시 생성자 수동 수정 불필요
// - Spring 4.3+ 이후 단일 생성자는 @Autowired 생략 가능

@Transactional(readOnly = true)
// 클래스 레벨 트랜잭션 설정: 모든 메서드를 읽기 전용 트랜잭션으로 실행
// 장점:
// - 읽기 전용 최적화: Hibernate가 변경 감지(Dirty Checking)를 수행하지 않아 성능 향상
// - 데이터베이스 성능 향상: DB는 읽기 전용 모드로 최적화 가능 (Oracle의 경우 읽기 일관성 유지)
// - 실수로 데이터 변경 방지: 읽기만 수행해야 하는 메서드의 안전성 보장
// - 쓰기가 필요한 메서드는 @Transactional(readOnly = false) 또는 @Transactional로 오버라이드
public class DashboardServiceImpl implements DashboardService {

    /**
     * 대시보드 관련 데이터 저장소
     * <p>컨테이너 카드, 스토리지 사용량 등 대시보드 전용 데이터 조회 담당</p>
     */
    private final DashboardRepository dashboardRepository;

    /**
     * 컨테이너 로그 데이터 저장소
     * <p>STDOUT, STDERR 로그 개수 집계 등 로그 관련 조회 담당</p>
     */
    private final ContainerLogRepository containerLogRepository;

    /**
     * 컨테이너 통계 로그 데이터 저장소
     * <p>CPU, 메모리, 네트워크, Block I/O 등 시계열 메트릭 데이터 조회 담당</p>
     */
    private final ContainerStatsLogRepository containerStatsLogRepository;

    /**
     * 컨테이너 기본 정보 저장소
     * <p>컨테이너 기본 정보 조회 및 검증 담당</p>
     */
    private final ContainerRepository containerRepository;

    /**
     * 전체 컨테이너 목록 조회 (정렬 및 필터링 지원)
     *
     * <p><b>처리 흐름:</b></p>
     * <ol>
     *   <li>정렬 타입이 없으면 기본값(FAVORITE)으로 설정</li>
     *   <li>정렬 타입을 Repository 파라미터 문자열로 변환</li>
     *   <li>필터 활성화 여부에 따라 적절한 Repository 메서드 호출</li>
     *   <li>필터가 있으면 findContainerCardsWithFilters() 사용</li>
     *   <li>필터가 없으면 findAllContainerCardsForDashboard() 사용</li>
     * </ol>
     *
     * <p><b>핵심 패턴:</b></p>
     * <ul>
     *   <li><b>조건부 쿼리 패턴:</b> 필터 활성화 여부에 따라 동적으로 쿼리 선택하여 성능 최적화</li>
     *   <li><b>타입 변환 패턴:</b> Enum(ContainerSortType) → String으로 변환하여 네이티브 쿼리와 호환</li>
     *   <li><b>Null-Safe 처리:</b> 필터의 각 필드가 null일 경우 안전하게 처리</li>
     * </ul>
     *
     * <p><b>성능 최적화:</b></p>
     * <ul>
     *   <li>필터가 없을 때는 간단한 쿼리 사용 (WHERE 절 최소화)</li>
     *   <li>필터가 있을 때만 복잡한 WHERE 절 적용</li>
     *   <li>Repository 레벨에서 네이티브 쿼리로 직접 DTO 조회 (N+1 문제 방지)</li>
     * </ul>
     *
     * @param sortType 정렬 타입 (CPU_PERCENT, MEM_PERCENT, NAME, FAVORITE)
     * @param memberId 사용자 ID (즐겨찾기 판단용)
     * @param filter 필터 조건 (키워드, 상태, 헬스, 즐겨찾기, Agent ID)
     * @return 컨테이너 카드 목록 (정렬 및 필터링 적용됨)
     */
    @Override
    public List<ContainerCardResponseDTO> getAllContainers(ContainerSortType sortType, Long memberId, ContainerFilterDTO filter) {
        log.info("대시보드: 전체 컨테이너 목록 조회 (정렬: {}, memberId: {}, 필터: {})", sortType, memberId, filter != null);

        // 정렬 타입이 없으면 기본값으로 즐겨찾기 정렬 적용
        // 즐겨찾기 정렬: 즐겨찾기한 컨테이너를 먼저 보여주고, 나머지는 이름순
        if (sortType == null) {
            sortType = ContainerSortType.FAVORITE;
        }

        // 정렬 타입을 Repository 파라미터로 변환
        // Enum → String 변환 (네이티브 쿼리에서 사용하기 위함)
        String sort = convertSortType(sortType);

        // 필터가 있으면 필터링 적용
        // 조건부 쿼리 패턴: 필터 유무에 따라 다른 메서드 호출
        if (hasActiveFilter(filter)) {
            // 필터 조건이 있을 때: 복잡한 WHERE 절 포함된 쿼리 사용
            return dashboardRepository.findContainerCardsWithFilters(
                    memberId,
                    filter.getKeyword(),                      // 컨테이너 이름 검색
                    getFirstOrNull(filter.getStates()),       // 상태 필터 (현재 단일 값만 지원)
                    getFirstOrNull(filter.getHealths()),      // 헬스 필터 (현재 단일 값만 지원)
                    filter.getFavoriteOnly() != null && filter.getFavoriteOnly(),  // 즐겨찾기만 보기
                    getFirstOrNull(filter.getAgentIds()),     // Agent ID 필터 (현재 단일 값만 지원)
                    sort
            );
        } else {
            // 필터 조건이 없을 때: 간단한 쿼리 사용 (성능 최적화)
            return dashboardRepository.findAllContainerCardsForDashboard(memberId, sort);
        }
    }

    /**
     * ContainerSortType Enum을 Repository 정렬 문자열로 변환
     *
     * <p><b>변환 이유:</b></p>
     * <ul>
     *   <li>네이티브 쿼리에서는 Enum을 직접 사용할 수 없음</li>
     *   <li>Repository 레이어에서 ORDER BY 절에 사용하기 위해 문자열로 변환</li>
     * </ul>
     *
     * <p><b>패턴:</b></p>
     * <ul>
     *   <li>Java 14+ Switch Expression 사용 (간결하고 타입 안전)</li>
     *   <li>모든 Enum 값에 대한 처리 보장 (누락 시 컴파일 에러)</li>
     * </ul>
     *
     * @param sortType 정렬 타입 Enum
     * @return Repository에서 사용할 정렬 문자열
     */
    private String convertSortType(ContainerSortType sortType) {
        return switch (sortType) {
            case CPU_PERCENT -> "CPU";      // CPU 사용률 기준 정렬
            case MEM_PERCENT -> "MEM";      // 메모리 사용률 기준 정렬
            case NAME -> "NAME";            // 컨테이너 이름 기준 정렬
            case FAVORITE -> "FAVORITE";    // 즐겨찾기 우선 정렬
        };
    }

    /**
     * 리스트에서 첫 번째 값 추출 (null-safe)
     *
     * <p><b>사용 이유:</b></p>
     * <ul>
     *   <li>현재 시스템은 다중 선택 필터를 지원하지 않음 (향후 확장 가능성 고려)</li>
     *   <li>프론트엔드에서 List로 전달하지만, 백엔드는 첫 번째 값만 사용</li>
     *   <li>null이나 빈 리스트를 안전하게 처리</li>
     * </ul>
     *
     * <p><b>패턴:</b></p>
     * <ul>
     *   <li>Null-Safe 패턴: NPE(NullPointerException) 방지</li>
     *   <li>제네릭 메서드: 모든 타입에 재사용 가능</li>
     * </ul>
     *
     * @param list 값을 추출할 리스트
     * @param <T> 리스트의 요소 타입
     * @return 리스트의 첫 번째 값, 또는 null
     */
    private <T> T getFirstOrNull(List<T> list) {
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

    /**
     * 필터가 활성화되어 있는지 확인
     *
     * <p><b>활성화 조건:</b></p>
     * <ul>
     *   <li>키워드가 입력된 경우 (공백 제거 후 판단)</li>
     *   <li>즐겨찾기 필터가 true인 경우</li>
     *   <li>상태 필터가 있는 경우 (예: running, exited 등)</li>
     *   <li>헬스 필터가 있는 경우 (예: healthy, unhealthy 등)</li>
     *   <li>Agent ID 필터가 있는 경우</li>
     * </ul>
     *
     * <p><b>목적:</b></p>
     * <ul>
     *   <li>쿼리 최적화: 필터가 없으면 간단한 쿼리 사용</li>
     *   <li>조건부 로직: 필터 유무에 따라 다른 Repository 메서드 호출</li>
     * </ul>
     *
     * @param filter 필터 DTO
     * @return true: 필터가 활성화됨, false: 필터가 비활성화됨
     */
    private boolean hasActiveFilter(ContainerFilterDTO filter) {
        if (filter == null) {
            return false;
        }
        return (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()) ||
                (filter.getFavoriteOnly() != null && filter.getFavoriteOnly()) ||
                (filter.getStates() != null && !filter.getStates().isEmpty()) ||
                (filter.getHealths() != null && !filter.getHealths().isEmpty()) ||
                (filter.getAgentIds() != null && !filter.getAgentIds().isEmpty());
    }

    /**
     * Agent별 컨테이너 개수 집계 조회
     *
     * <p><b>역할:</b></p>
     * <ul>
     *   <li>각 Agent가 관리하는 컨테이너의 개수를 집계</li>
     *   <li>대시보드에서 Agent별 분포를 시각화하는데 사용</li>
     * </ul>
     *
     * <p><b>처리:</b></p>
     * <ul>
     *   <li>Repository에서 GROUP BY 쿼리로 집계 결과를 직접 DTO로 반환</li>
     *   <li>비즈니스 로직 없이 단순 위임 (Delegation Pattern)</li>
     * </ul>
     *
     * <p><b>DTO 변환 패턴:</b></p>
     * <ul>
     *   <li>Repository 레벨에서 집계 쿼리 결과를 바로 DTO로 변환</li>
     *   <li>Service 레벨에서 추가 변환 불필요 (성능 최적화)</li>
     * </ul>
     *
     * @return Agent별 컨테이너 개수 목록
     */
    @Override
    public List<AgentContainerCountDTO> getContainerCountByAgent() {
        log.info("대시보드: Agent별 컨테이너 개수 집계");
        return dashboardRepository.countContainersByAgent();
    }

    /**
     * 당일 STDOUT/STDERR 로그 개수 조회
     *
     * <p><b>처리 흐름:</b></p>
     * <ol>
     *   <li>당일 0시(startOfDay) 계산</li>
     *   <li>다음날 0시(startOfNextDay) 계산 → 시간 범위: [오늘 0시, 내일 0시)</li>
     *   <li>STDOUT 로그 개수 조회 (표준 출력)</li>
     *   <li>STDERR 로그 개수 조회 (표준 에러)</li>
     *   <li>조회 기준일을 ISO_LOCAL_DATE 형식으로 변환 (YYYY-MM-DD)</li>
     *   <li>DailyLogCountDTO로 응답 생성</li>
     * </ol>
     *
     * <p><b>핵심 개념:</b></p>
     * <ul>
     *   <li><b>LogSource 구분:</b> STDOUT(정상 로그), STDERR(에러 로그) 분리 집계</li>
     *   <li><b>시간 범위:</b> [시작, 종료) 반개구간 사용 (종료 시간 미포함)</li>
     *   <li><b>날짜 계산:</b> LocalDate.now().atStartOfDay()로 오늘 0시 정확히 계산</li>
     * </ul>
     *
     * <p><b>Builder 패턴 사용:</b></p>
     * <ul>
     *   <li>Lombok의 @Builder로 가독성 높은 객체 생성</li>
     *   <li>필드가 많을 때 유용 (어떤 값이 어떤 필드에 들어가는지 명확)</li>
     * </ul>
     *
     * @return 당일 로그 개수 (STDOUT, STDERR 분리, 기준일 포함)
     */
    @Override
    public DailyLogCountDTO getDailyLogCount() {
        log.info("대시보드: 당일 0시 기준 STDOUT/STDERR 로그 개수 조회");

        // 당일 0시 계산
        // LocalDate.now(): 오늘 날짜 (시간 정보 없음)
        // atStartOfDay(): 오늘 날짜의 00:00:00
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        // 다음날 0시 계산
        // plusDays(1): 하루 추가 → 내일 00:00:00
        // 범위: [오늘 0시, 내일 0시) → 오늘 하루 전체
        LocalDateTime startOfNextDay = startOfDay.plusDays(1);

        // STDOUT 로그 개수 조회
        // STDOUT: 표준 출력 (일반 로그, 정보성 메시지)
        long stdoutCount = containerLogRepository.countBySourceAndLoggedAtBetween(
                LogSource.STDOUT,
                startOfDay,
                startOfNextDay
        );

        // STDERR 로그 개수 조회
        // STDERR: 표준 에러 (에러 로그, 경고 메시지)
        long stderrCount = containerLogRepository.countBySourceAndLoggedAtBetween(
                LogSource.STDERR,
                startOfDay,
                startOfNextDay
        );

        // 조회 기준일 (YYYY-MM-DD 형식)
        // DateTimeFormatter.ISO_LOCAL_DATE: ISO 8601 표준 형식 (예: 2025-11-24)
        String date = startOfDay.format(DateTimeFormatter.ISO_LOCAL_DATE);

        log.info("당일 로그 개수 - STDOUT: {}, STDERR: {}, 기준일: {}", stdoutCount, stderrCount, date);

        // Builder 패턴으로 DTO 생성
        // 장점: 가독성 높음, null 안전, 필드 순서 무관
        return DailyLogCountDTO.builder()
                .stdoutCount(stdoutCount)
                .stderrCount(stderrCount)
                .date(date)
                .build();
    }

    /**
     * 전체 컨테이너 스토리지 사용량 조회
     *
     * <p><b>역할:</b></p>
     * <ul>
     *   <li>모든 컨테이너의 스토리지 사용량을 조회</li>
     *   <li>대시보드에서 스토리지 사용 현황을 시각화하는데 사용</li>
     * </ul>
     *
     * <p><b>처리:</b></p>
     * <ul>
     *   <li>Repository에서 최신 스토리지 정보를 직접 DTO로 반환</li>
     *   <li>단순 위임 (Delegation Pattern)</li>
     * </ul>
     *
     * <p><b>DTO 변환 패턴:</b></p>
     * <ul>
     *   <li>Repository 레벨에서 쿼리 결과를 바로 DTO로 변환</li>
     *   <li>Service 레벨에서 추가 변환 불필요 (성능 최적화)</li>
     * </ul>
     *
     * @return 전체 컨테이너 스토리지 사용량 목록
     */
    @Override
    public List<ContainerStorageUsageDTO> getAllContainerStorageUsage() {
        log.info("대시보드: 전체 컨테이너 스토리지 사용량 조회");
        return dashboardRepository.findAllContainerStorageUsage();
    }

    /**
     * 네트워크 통계 시계열 데이터 조회
     *
     * <p><b>처리 흐름:</b></p>
     * <ol>
     *   <li>컨테이너 정보 조회 및 검증 (없으면 예외 발생)</li>
     *   <li>시간 범위 계산: 현재 시간 - timeRange(분)</li>
     *   <li>해당 기간의 모든 통계 데이터 조회</li>
     *   <li>detail 플래그에 따라 샘플링 포인트 개수 결정 (200 또는 50)</li>
     *   <li>샘플링 처리: 원본 데이터를 목표 포인트 개수로 다운샘플링</li>
     *   <li>NetworkStatsTimeSeriesDTO로 응답 생성</li>
     * </ol>
     *
     * <p><b>핵심 패턴:</b></p>
     * <ul>
     *   <li><b>샘플링 패턴:</b> 대량의 시계열 데이터를 일정 개수로 압축하여 네트워크 전송량 최적화</li>
     *   <li><b>조건부 샘플링:</b> detail 플래그에 따라 상세도 조절 (고해상도/저해상도)</li>
     *   <li><b>시간 범위 계산:</b> TimeRange Enum으로 다양한 기간 지원 (1시간, 24시간, 7일 등)</li>
     * </ul>
     *
     * <p><b>성능 최적화:</b></p>
     * <ul>
     *   <li>샘플링으로 데이터 포인트 개수 제한 (detail: 200, 일반: 50)</li>
     *   <li>클라이언트 렌더링 부하 감소, 네트워크 전송량 감소</li>
     * </ul>
     *
     * @param containerId 컨테이너 ID
     * @param timeRange 시간 범위 (1시간, 24시간, 7일 등)
     * @param detail 상세 모드 (true: 200 포인트, false: 50 포인트)
     * @return 네트워크 통계 시계열 데이터 (샘플링 적용됨)
     * @throws IllegalArgumentException 컨테이너를 찾을 수 없는 경우
     */
    @Override
    public NetworkStatsTimeSeriesDTO getNetworkStatsTimeSeries(Long containerId, TimeRange timeRange, boolean detail) {
        log.info("대시보드: 네트워크 통계 시계열 데이터 조회 - containerId: {}, timeRange: {}, detail: {}",
                containerId, timeRange, detail);

        // 컨테이너 정보 조회 및 검증
        // Optional.orElseThrow(): 값이 없으면 예외 발생 (Null 안전)
        var container = containerRepository.findById(containerId)
                .orElseThrow(() -> new IllegalArgumentException("컨테이너를 찾을 수 없습니다: " + containerId));

        // 시간 범위 계산
        // LocalDateTime.now(): 현재 시간
        // minusMinutes(): 지정된 분만큼 과거로 이동
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.minusMinutes(timeRange.getMinutes());

        // 통계 데이터 조회
        // 지정된 시간 범위 내의 모든 ContainerStatsLog 조회
        List<ContainerStatsLog> statsLogs = containerStatsLogRepository.findByContainerIdAndTimeRange(
                containerId, startTime, now
        );

        // 샘플링 처리
        // detail 플래그에 따라 목표 포인트 개수 결정
        // - detail=true: 200 포인트 (고해상도, 더 정밀한 그래프)
        // - detail=false: 50 포인트 (저해상도, 빠른 로딩)
        int targetPoints = detail ? 200 : 50;
        List<NetworkStatsDataPointDTO> dataPoints = sampleNetworkStats(statsLogs, targetPoints);

        // Builder 패턴으로 응답 생성
        return NetworkStatsTimeSeriesDTO.builder()
                .containerId(containerId)
                .containerName(container.getName())
                .timeRange(timeRange)
                .dataPoints(dataPoints)
                .build();
    }

    /**
     * 네트워크 통계 데이터 샘플링 (다운샘플링)
     *
     * <p><b>샘플링 알고리즘:</b></p>
     * <ol>
     *   <li>원본 데이터가 비어있으면 빈 리스트 반환</li>
     *   <li>원본 데이터가 목표 포인트보다 적으면 그대로 반환 (샘플링 불필요)</li>
     *   <li>샘플링 간격(step) 계산: 원본 데이터 개수 / 목표 포인트 개수</li>
     *   <li>균등 간격으로 데이터 포인트 추출 (인덱스 기반 샘플링)</li>
     * </ol>
     *
     * <p><b>샘플링 방식:</b></p>
     * <ul>
     *   <li><b>균등 간격 샘플링:</b> step 간격마다 데이터 포인트 선택</li>
     *   <li>예시: 1000개 데이터를 50개로 샘플링 → step = 20 → 0번, 20번, 40번... 인덱스 선택</li>
     *   <li>시간 분포가 균등하게 유지됨 (시각화에 유리)</li>
     * </ul>
     *
     * <p><b>DTO 변환 패턴:</b></p>
     * <ul>
     *   <li>Entity(ContainerStatsLog) → DTO(NetworkStatsDataPointDTO) 변환</li>
     *   <li>Stream API와 Builder 패턴으로 간결하게 처리</li>
     *   <li>필요한 필드만 추출 (timestamp, rxBytesPerSec, txBytesPerSec)</li>
     * </ul>
     *
     * <p><b>성능 최적화:</b></p>
     * <ul>
     *   <li>대량의 데이터를 목표 개수로 압축하여 네트워크 전송량 감소</li>
     *   <li>클라이언트 렌더링 부하 감소 (차트 렌더링 시 성능 향상)</li>
     * </ul>
     *
     * @param statsLogs 원본 통계 로그 (시간순 정렬 가정)
     * @param targetPoints 목표 포인트 개수 (detail=true: 200, detail=false: 50)
     * @return 샘플링된 데이터 포인트 목록
     */
    private List<NetworkStatsDataPointDTO> sampleNetworkStats(List<ContainerStatsLog> statsLogs, int targetPoints) {
        // 1. 빈 데이터 처리
        if (statsLogs.isEmpty()) {
            return List.of();
        }

        // 2. 원본 데이터가 목표보다 적으면 그대로 반환
        // 샘플링 불필요: 데이터가 이미 충분히 적음
        if (statsLogs.size() <= targetPoints) {
            // Stream API로 Entity → DTO 변환
            return statsLogs.stream()
                    .map(log -> NetworkStatsDataPointDTO.builder()
                            .timestamp(log.getCollectedAt())          // 수집 시간
                            .rxBytesPerSec(log.getRxBytesPerSec())    // 수신 바이트/초
                            .txBytesPerSec(log.getTxBytesPerSec())    // 송신 바이트/초
                            .build())
                    .collect(Collectors.toList());
        }

        // 3. 샘플링 간격 계산
        // step: 원본 데이터에서 몇 개마다 하나씩 선택할지 결정
        // 예: 1000개 → 50개로 샘플링 시 step = 20.0 → 0, 20, 40, 60... 인덱스 선택
        double step = (double) statsLogs.size() / targetPoints;
        List<NetworkStatsDataPointDTO> sampledData = new ArrayList<>();

        // 4. 균등 간격으로 데이터 포인트 추출
        for (int i = 0; i < targetPoints; i++) {
            int index = (int) (i * step);  // 샘플링할 인덱스 계산
            if (index < statsLogs.size()) {
                ContainerStatsLog log = statsLogs.get(index);
                // Entity → DTO 변환
                sampledData.add(NetworkStatsDataPointDTO.builder()
                        .timestamp(log.getCollectedAt())
                        .rxBytesPerSec(log.getRxBytesPerSec())
                        .txBytesPerSec(log.getTxBytesPerSec())
                        .build());
            }
        }

        return sampledData;
    }

    /**
     * Block I/O 통계 시계열 데이터 조회
     *
     * <p><b>처리 흐름:</b></p>
     * <ol>
     *   <li>컨테이너 정보 조회 및 검증 (없으면 예외 발생)</li>
     *   <li>시간 범위 계산: 현재 시간 - timeRange(분)</li>
     *   <li>해당 기간의 모든 통계 데이터 조회</li>
     *   <li>detail 플래그에 따라 샘플링 포인트 개수 결정 (200 또는 50)</li>
     *   <li>샘플링 처리: 원본 데이터를 목표 포인트 개수로 다운샘플링</li>
     *   <li>BlockIOStatsTimeSeriesDTO로 응답 생성</li>
     * </ol>
     *
     * <p><b>핵심 패턴:</b></p>
     * <ul>
     *   <li><b>샘플링 패턴:</b> 네트워크 통계와 동일한 알고리즘 사용 (코드 재사용)</li>
     *   <li><b>조건부 샘플링:</b> detail 플래그로 상세도 조절</li>
     *   <li><b>Block I/O 메트릭:</b> 디스크 읽기/쓰기 성능 모니터링</li>
     * </ul>
     *
     * <p><b>Block I/O란?</b></p>
     * <ul>
     *   <li>블록 단위의 입출력 (디스크 I/O)</li>
     *   <li>blkRead: 누적 읽기 바이트</li>
     *   <li>blkWrite: 누적 쓰기 바이트</li>
     *   <li>blkReadPerSec: 초당 읽기 바이트 (성능 지표)</li>
     *   <li>blkWritePerSec: 초당 쓰기 바이트 (성능 지표)</li>
     * </ul>
     *
     * @param containerId 컨테이너 ID
     * @param timeRange 시간 범위 (1시간, 24시간, 7일 등)
     * @param detail 상세 모드 (true: 200 포인트, false: 50 포인트)
     * @return Block I/O 통계 시계열 데이터 (샘플링 적용됨)
     * @throws IllegalArgumentException 컨테이너를 찾을 수 없는 경우
     */
    @Override
    public BlockIOStatsTimeSeriesDTO getBlockIOStatsTimeSeries(Long containerId, TimeRange timeRange, boolean detail) {
        log.info("대시보드: Block I/O 통계 시계열 데이터 조회 - containerId: {}, timeRange: {}, detail: {}",
                containerId, timeRange, detail);

        // 컨테이너 정보 조회 및 검증
        var container = containerRepository.findById(containerId)
                .orElseThrow(() -> new IllegalArgumentException("컨테이너를 찾을 수 없습니다: " + containerId));

        // 시간 범위 계산
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.minusMinutes(timeRange.getMinutes());

        // 통계 데이터 조회
        List<ContainerStatsLog> statsLogs = containerStatsLogRepository.findByContainerIdAndTimeRange(
                containerId, startTime, now
        );

        // 샘플링 처리
        // 네트워크 통계와 동일한 샘플링 전략 사용
        int targetPoints = detail ? 200 : 50;
        List<BlockIOStatsDataPointDTO> dataPoints = sampleBlockIOStats(statsLogs, targetPoints);

        // Builder 패턴으로 응답 생성
        return BlockIOStatsTimeSeriesDTO.builder()
                .containerId(containerId)
                .containerName(container.getName())
                .timeRange(timeRange)
                .dataPointCount(dataPoints.size())  // 실제 반환된 데이터 포인트 개수
                .dataPoints(dataPoints)
                .build();
    }

    /**
     * Block I/O 통계 데이터 샘플링 (다운샘플링)
     *
     * <p><b>샘플링 알고리즘:</b></p>
     * <ol>
     *   <li>원본 데이터가 비어있으면 빈 리스트 반환</li>
     *   <li>원본 데이터가 목표 포인트보다 적으면 그대로 반환 (샘플링 불필요)</li>
     *   <li>샘플링 간격(step) 계산: 원본 데이터 개수 / 목표 포인트 개수</li>
     *   <li>균등 간격으로 데이터 포인트 추출 (인덱스 기반 샘플링)</li>
     * </ol>
     *
     * <p><b>네트워크 샘플링과의 차이점:</b></p>
     * <ul>
     *   <li>알고리즘은 동일하지만, 추출하는 필드가 다름</li>
     *   <li>네트워크: rxBytesPerSec, txBytesPerSec</li>
     *   <li>Block I/O: blkRead, blkWrite, blkReadPerSec, blkWritePerSec (4개 필드)</li>
     * </ul>
     *
     * <p><b>DTO 변환 패턴:</b></p>
     * <ul>
     *   <li>Entity(ContainerStatsLog) → DTO(BlockIOStatsDataPointDTO) 변환</li>
     *   <li>Stream API와 Builder 패턴으로 간결하게 처리</li>
     *   <li>Block I/O 관련 필드만 추출 (timestamp + 4개 메트릭)</li>
     * </ul>
     *
     * <p><b>추출 필드 설명:</b></p>
     * <ul>
     *   <li><b>blkRead:</b> 컨테이너 시작 이후 누적 읽기 바이트 (총량)</li>
     *   <li><b>blkWrite:</b> 컨테이너 시작 이후 누적 쓰기 바이트 (총량)</li>
     *   <li><b>blkReadPerSec:</b> 초당 읽기 바이트 (실시간 성능 지표)</li>
     *   <li><b>blkWritePerSec:</b> 초당 쓰기 바이트 (실시간 성능 지표)</li>
     * </ul>
     *
     * @param statsLogs 원본 통계 로그 (시간순 정렬 가정)
     * @param targetPoints 목표 포인트 개수 (detail=true: 200, detail=false: 50)
     * @return 샘플링된 데이터 포인트 목록
     */
    private List<BlockIOStatsDataPointDTO> sampleBlockIOStats(List<ContainerStatsLog> statsLogs, int targetPoints) {
        // 1. 빈 데이터 처리
        if (statsLogs.isEmpty()) {
            return List.of();
        }

        // 2. 원본 데이터가 목표보다 적으면 그대로 반환
        if (statsLogs.size() <= targetPoints) {
            // Stream API로 Entity → DTO 변환
            return statsLogs.stream()
                    .map(log -> BlockIOStatsDataPointDTO.builder()
                            .timestamp(log.getCollectedAt())              // 수집 시간
                            .blkRead(log.getBlkRead())                    // 누적 읽기 바이트
                            .blkWrite(log.getBlkWrite())                  // 누적 쓰기 바이트
                            .blkReadPerSec(log.getBlkReadPerSec())        // 초당 읽기 바이트
                            .blkWritePerSec(log.getBlkWritePerSec())      // 초당 쓰기 바이트
                            .build())
                    .collect(Collectors.toList());
        }

        // 3. 샘플링 간격 계산
        double step = (double) statsLogs.size() / targetPoints;
        List<BlockIOStatsDataPointDTO> sampledData = new ArrayList<>();

        // 4. 균등 간격으로 데이터 포인트 추출
        for (int i = 0; i < targetPoints; i++) {
            int index = (int) (i * step);
            if (index < statsLogs.size()) {
                ContainerStatsLog log = statsLogs.get(index);
                // Entity → DTO 변환
                sampledData.add(BlockIOStatsDataPointDTO.builder()
                        .timestamp(log.getCollectedAt())
                        .blkRead(log.getBlkRead())
                        .blkWrite(log.getBlkWrite())
                        .blkReadPerSec(log.getBlkReadPerSec())
                        .blkWritePerSec(log.getBlkWritePerSec())
                        .build());
            }
        }

        return sampledData;
    }

    /**
     * 컨테이너 상세 메트릭 조회
     *
     * <p><b>처리 흐름:</b></p>
     * <ol>
     *   <li>clientDate가 null이면 서버 현재 날짜 사용</li>
     *   <li>컨테이너 기본 정보 조회 (없으면 NotFoundException 발생)</li>
     *   <li>최신 StatsLog 조회 (1시간 이내의 가장 최근 데이터)</li>
     *   <li>DashboardContainerDetailDTO 생성 (팩토리 메서드 패턴 사용)</li>
     * </ol>
     *
     * <p><b>핵심 패턴:</b></p>
     * <ul>
     *   <li><b>팩토리 메서드 패턴:</b> DTO의 정적 팩토리 메서드로 복잡한 객체 생성 위임</li>
     *   <li><b>Null-Safe 처리:</b> clientDate가 null일 경우 서버 시간 사용 (클라이언트 타임존 이슈 방지)</li>
     *   <li><b>파티션 프루닝:</b> 1시간 전부터 조회하여 DB 성능 최적화 (Oracle 파티션 테이블)</li>
     * </ul>
     *
     * <p><b>예외 처리:</b></p>
     * <ul>
     *   <li><b>CONTAINER_NOT_FOUND:</b> 존재하지 않는 컨테이너 ID 요청 시</li>
     *   <li><b>CONTAINER_STATS_LOG_NOT_FOUND:</b> 통계 데이터가 없는 경우 (Agent 미수집 등)</li>
     * </ul>
     *
     * <p><b>팩토리 메서드 사용 이유:</b></p>
     * <ul>
     *   <li>DTO 생성 로직이 복잡함 (로그 집계, 스토리지 조회 등 다양한 Repository 사용)</li>
     *   <li>Service 레벨에서 세부 로직을 감춤 (캡슐화)</li>
     *   <li>forRealtimeUpdateWithMetrics(): 최초 API 호출용 (전체 데이터 포함)</li>
     * </ul>
     *
     * <p><b>파티션 프루닝이란?</b></p>
     * <ul>
     *   <li>Oracle 파티션 테이블에서 불필요한 파티션 스캔 방지</li>
     *   <li>ContainerStatsLog는 시간 기반 파티션 (1시간 단위 등)</li>
     *   <li>1시간 전부터 조회 → 최소 1~2개 파티션만 스캔 (전체 테이블 스캔 방지)</li>
     *   <li>성능 향상: 인덱스 스캔 + 파티션 프루닝으로 빠른 조회</li>
     * </ul>
     *
     * @param containerId 컨테이너 ID
     * @param clientDate 클라이언트 날짜 (로그 집계 기준일, null이면 서버 시간 사용)
     * @return 컨테이너 상세 메트릭 (CPU, 메모리, 네트워크, Block I/O, 로그, 스토리지 포함)
     * @throws NotFoundException 컨테이너 또는 통계 로그를 찾을 수 없는 경우
     */
    @Override
    public DashboardContainerDetailDTO getContainerDetailMetrics(Long containerId, LocalDate clientDate) {
        log.info("컨테이너 상세 메트릭 조회 - containerId: {}, clientDate: {}", containerId, clientDate);

        // clientDate가 null이면 서버 시간 사용
        // 이유: 클라이언트 타임존 차이로 인한 날짜 불일치 방지
        LocalDate dateToUse = clientDate != null ? clientDate : LocalDate.now();

        // 컨테이너 조회
        // Optional.orElseThrow(): 없으면 커스텀 예외 발생
        Container container = containerRepository.findById(containerId)
                .orElseThrow(() -> new NotFoundException(
                        ExceptionMessage.CONTAINER_NOT_FOUND));

        // 최신 StatsLog 조회 - 파티션 프루닝을 위해 1시간 전부터 조회
        // 파티션 프루닝: Oracle 파티션 테이블에서 특정 파티션만 스캔하여 성능 최적화
        // minusHours(1): 1시간 전부터 조회 → 최신 데이터가 있는 파티션만 스캔
        ContainerStatsLog statsLog = containerStatsLogRepository.findLatestByContainerId(
                        containerId,
                        LocalDateTime.now().minusHours(1)
                )
                .orElseThrow(() -> new NotFoundException(
                        ExceptionMessage.CONTAINER_STATS_LOG_NOT_FOUND));

        // DashboardContainerDetailDTO 생성 (최초 API 호출용 - 로그, 스토리지 포함)
        // 팩토리 메서드 패턴: 복잡한 DTO 생성 로직을 DTO 내부로 캡슐화
        // forRealtimeUpdateWithMetrics():
        //   - 컨테이너 기본 정보
        //   - Agent 정보
        //   - 최신 통계 데이터 (CPU, 메모리, 네트워크, Block I/O)
        //   - 당일 로그 개수 (containerLogRepository 사용)
        //   - 스토리지 사용량 (dashboardRepository 사용)
        return DashboardContainerDetailDTO.forRealtimeUpdateWithMetrics(
                container,
                container.getAgent(),
                statsLog,
                containerLogRepository,
                dashboardRepository,
                dateToUse
        );
    }
}

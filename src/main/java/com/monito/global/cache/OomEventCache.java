/**
 * OOM 이벤트 하이브리드 캐시
 * - 인메모리 캐시: 빠른 조회
 * - DB 저장: 서버 재시작 시 복구
 * - 7일 보관, 자동 정리
 */
package com.monito.global.cache;

import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.OomEventEntity;
import com.monito.domains.container.repository.ContainerRepository;
import com.monito.domains.container.repository.OomEventRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 작성자: 백승준
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OomEventCache {

    private final OomEventRepository oomEventRepository;
    private final ContainerRepository containerRepository;

    /**
     * 이벤트 보관 기간 (7일)
     */
    private static final Duration RETENTION_PERIOD = Duration.ofDays(7);

    /**
     * Key: containerId, Value: OOM 이벤트 리스트
     * - 시간 순으로 정렬된 상태 유지
     */
    private final ConcurrentHashMap<Long, List<OomEvent>> eventsByContainer = new ConcurrentHashMap<>();

    /**
     * 전체 OOM 이벤트 (시간대별 집계용)
     * - 시간 순으로 정렬된 모든 이벤트
     */
    private final List<OomEvent> allEvents = Collections.synchronizedList(new ArrayList<>());

    /**
     * 서버 시작 시 DB에서 최근 7일 OOM 이벤트 로드
     */
    @PostConstruct
    public void init() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minus(RETENTION_PERIOD);
        List<OomEventEntity> recentEvents = oomEventRepository.findAllAfter(sevenDaysAgo);

        for (OomEventEntity entity : recentEvents) {
            OomEvent cacheEvent = entity.toCacheEvent();

            // 캐시에 추가
            eventsByContainer.computeIfAbsent(cacheEvent.getContainerId(), k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(cacheEvent);
            allEvents.add(cacheEvent);
        }

        log.info("[OOM_CACHE] 초기화 완료 - 로드된 이벤트: {}건, 컨테이너: {}개",
                allEvents.size(), eventsByContainer.size());
    }

    /**
     * OOM 이벤트 기록 (캐시 + DB)
     * @param event OOM 이벤트
     */
    public void recordEvent(OomEvent event) {
        // 1. 캐시에 추가
        eventsByContainer.compute(event.getContainerId(), (k, events) -> {
            if (events == null) {
                events = Collections.synchronizedList(new ArrayList<>());
            }
            events.add(event);
            return events;
        });
        allEvents.add(event);

        log.info("[OOM_CACHE] 이벤트 기록 - containerId: {}, containerName: {}, occurredAt: {}",
                event.getContainerId(), event.getContainerName(), event.getOccurredAt());

        // 2. DB에 비동기 저장
        saveToDatabase(event);

        // 3. 주기적 정리 (7일 이전 데이터 삭제)
        cleanupExpiredEvents();
    }

    /**
     * DB에 비동기 저장
     */
    @Async
    @Transactional
    protected void saveToDatabase(OomEvent event) {
        try {
            Container container = containerRepository.findById(event.getContainerId())
                    .orElse(null);

            if (container == null) {
                log.warn("[OOM_CACHE] 컨테이너를 찾을 수 없음 - containerId: {}", event.getContainerId());
                return;
            }

            OomEventEntity entity = OomEventEntity.from(event, container);
            oomEventRepository.save(entity);

            log.debug("[OOM_CACHE] DB 저장 완료 - containerId: {}", event.getContainerId());
        } catch (Exception e) {
            log.error("[OOM_CACHE] DB 저장 실패 - containerId: {}, error: {}",
                    event.getContainerId(), e.getMessage(), e);
        }
    }

    /**
     * 특정 컨테이너의 OOM 이벤트 조회
     * @param containerId 컨테이너 ID
     * @param startTime 조회 시작 시간
     * @param endTime 조회 종료 시간
     * @return OOM 이벤트 리스트
     */
    public List<OomEvent> getEvents(Long containerId, LocalDateTime startTime, LocalDateTime endTime) {
        List<OomEvent> events = eventsByContainer.get(containerId);
        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }

        return events.stream()
                .filter(e -> !e.getOccurredAt().isBefore(startTime) && !e.getOccurredAt().isAfter(endTime))
                .sorted(Comparator.comparing(OomEvent::getOccurredAt))
                .collect(Collectors.toList());
    }

    /**
     * 특정 컨테이너의 최근 7일 OOM 이벤트 조회
     * @param containerId 컨테이너 ID
     * @return OOM 이벤트 리스트
     */
    public List<OomEvent> getRecentEvents(Long containerId) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minus(RETENTION_PERIOD);
        return getEvents(containerId, sevenDaysAgo, LocalDateTime.now());
    }

    /**
     * 시간대별 OOM 히스토그램 생성
     * @param containerId 컨테이너 ID
     * @param startTime 조회 시작 시간
     * @param endTime 조회 종료 시간
     * @param bucketSize 버킷 크기 (예: 1시간, 1일)
     * @return 시간대별 OOM 발생 횟수 (Key: 시간대, Value: 발생 횟수)
     */
    public Map<LocalDateTime, Long> getHistogram(
            Long containerId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            ChronoUnit bucketSize
    ) {
        List<OomEvent> events = getEvents(containerId, startTime, endTime);

        return events.stream()
                .collect(Collectors.groupingBy(
                        event -> truncateTime(event.getOccurredAt(), bucketSize),
                        Collectors.counting()
                ));
    }

    /**
     * 전체 컨테이너 대상 시간대별 OOM 히스토그램 (Heatmap용)
     * @param startTime 조회 시작 시간
     * @param endTime 조회 종료 시간
     * @param bucketSize 버킷 크기
     * @return 시간대별 OOM 발생 횟수
     */
    public Map<LocalDateTime, Long> getGlobalHistogram(
            LocalDateTime startTime,
            LocalDateTime endTime,
            ChronoUnit bucketSize
    ) {
        return allEvents.stream()
                .filter(e -> !e.getOccurredAt().isBefore(startTime) && !e.getOccurredAt().isAfter(endTime))
                .collect(Collectors.groupingBy(
                        event -> truncateTime(event.getOccurredAt(), bucketSize),
                        Collectors.counting()
                ));
    }

    /**
     * 특정 컨테이너의 최근 7일 OOM 발생 횟수
     * @param containerId 컨테이너 ID
     * @return 최근 7일 OOM 횟수
     */
    public int getRecentOomCount(Long containerId) {
        return getRecentEvents(containerId).size();
    }

    /**
     * 특정 컨테이너의 마지막 OOM 발생 시각
     * @param containerId 컨테이너 ID
     * @return 마지막 OOM 발생 시각 (없으면 null)
     */
    public LocalDateTime getLastOomTime(Long containerId) {
        List<OomEvent> events = eventsByContainer.get(containerId);
        if (events == null || events.isEmpty()) {
            return null;
        }

        return events.stream()
                .map(OomEvent::getOccurredAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    /**
     * 7일 이전 이벤트 삭제 (메모리 관리)
     */
    private void cleanupExpiredEvents() {
        LocalDateTime cutoffTime = LocalDateTime.now().minus(RETENTION_PERIOD);

        // 1. 컨테이너별 이벤트 정리
        eventsByContainer.forEach((containerId, events) -> {
            events.removeIf(event -> event.getOccurredAt().isBefore(cutoffTime));
        });

        // 2. 빈 리스트 삭제
        eventsByContainer.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        // 3. 전체 이벤트 정리
        int beforeSize = allEvents.size();
        allEvents.removeIf(event -> event.getOccurredAt().isBefore(cutoffTime));
        int afterSize = allEvents.size();

        if (beforeSize > afterSize) {
            log.info("[OOM_CACHE] 만료된 이벤트 삭제 - 삭제: {}건, 남은: {}건", beforeSize - afterSize, afterSize);
        }
    }

    /**
     * 시간 절삭 (버킷 크기에 맞춰)
     * @param time 원본 시간
     * @param unit 절삭 단위 (HOURS, DAYS 등)
     * @return 절삭된 시간
     */
    private LocalDateTime truncateTime(LocalDateTime time, ChronoUnit unit) {
        return switch (unit) {
            case HOURS -> time.truncatedTo(ChronoUnit.HOURS);
            case DAYS -> time.truncatedTo(ChronoUnit.DAYS);
            case MINUTES -> time.truncatedTo(ChronoUnit.MINUTES);
            default -> time;
        };
    }

    /**
     * 특정 컨테이너의 캐시 삭제 (컨테이너 삭제 시)
     * @param containerId 컨테이너 ID
     */
    public void removeContainer(Long containerId) {
        List<OomEvent> removed = eventsByContainer.remove(containerId);
        if (removed != null && !removed.isEmpty()) {
            // 전체 이벤트에서도 제거
            allEvents.removeIf(event -> event.getContainerId().equals(containerId));
            log.info("[OOM_CACHE] 컨테이너 이벤트 삭제 - containerId: {}, 삭제된 이벤트: {}건", containerId, removed.size());
        }
    }

    /**
     * 전체 캐시 초기화 (테스트용)
     */
    public void clear() {
        eventsByContainer.clear();
        allEvents.clear();
        log.warn("[OOM_CACHE] 전체 OOM 이벤트 캐시 초기화");
    }

    /**
     * 캐시 통계
     * @return 캐시 상태 정보
     */
    public Map<String, Object> getStats() {
        return Map.of(
                "totalContainers", eventsByContainer.size(),
                "totalEvents", allEvents.size(),
                "retentionDays", RETENTION_PERIOD.toDays()
        );
    }
}
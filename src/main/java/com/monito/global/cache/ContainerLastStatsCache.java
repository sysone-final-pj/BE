/**
 * 컨테이너별 최신 통계 캐시
 * - DB 조회 없이 이전 통계 데이터 제공
 * - 메트릭 계산 성능 최적화 (delta 계산용)
 */
package com.monito.global.cache;

import com.monito.domains.container.domain.ContainerStatsLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
 작성자: 백승준
 */
@Component
@Slf4j
public class ContainerLastStatsCache {

    private final Map<String, ContainerStatsLog> cache = new ConcurrentHashMap<>();

    /**
     * 컨테이너의 최신 통계 조회
     * @param containerHash 컨테이너 해시
     * @return 캐시된 최신 통계 (없으면 null)
     */
    public ContainerStatsLog get(String containerHash) {
        return cache.get(containerHash);
    }

    /**
     * 컨테이너의 최신 통계 저장
     * @param containerHash 컨테이너 해시
     * @param stats 통계 로그
     */
    public void put(String containerHash, ContainerStatsLog stats) {
        cache.put(containerHash, stats);
        log.debug("최신 통계 캐시 업데이트 - containerHash: {}", containerHash);
    }

    /**
     * 특정 컨테이너 통계 제거
     * @param containerHash 컨테이너 해시
     */
    public void remove(String containerHash) {
        cache.remove(containerHash);
        log.info("통계 캐시 제거 - containerHash: {}", containerHash);
    }

    /**
     * 모든 캐시 삭제
     */
    public void clear() {
        int size = cache.size();
        cache.clear();
        log.info("전체 통계 캐시 삭제 - 삭제된 항목 수: {}", size);
    }

    /**
     * 캐시 크기 반환
     */
    public int size() {
        return cache.size();
    }
}

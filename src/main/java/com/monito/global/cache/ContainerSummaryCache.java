/**
 * 컨테이너 요약 정보 캐시 (사용자별 상태 없음)
 * - Snapshot: 순수 컨테이너 데이터만 저장
 * - 모든 사용자가 공유하는 캐시
 */
package com.monito.global.cache;

import com.monito.domains.container.dto.response.ContainerSummarySnapshot;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 작성자: 백승준
 */
@Component
public class ContainerSummaryCache {

    private final ConcurrentHashMap<Long, ContainerSummarySnapshot> cache = new ConcurrentHashMap<>();

    /**
     * 캐시 업데이트
     */
    public void update(ContainerSummarySnapshot snapshot) {
        cache.put(snapshot.getId(), snapshot);
    }

    /**
     * 모든 스냅샷 조회
     */
    public List<ContainerSummarySnapshot> getAllSnapshots() {
        return List.copyOf(cache.values());
    }

    /**
     * 특정 컨테이너 스냅샷 조회
     */
    public Optional<ContainerSummarySnapshot> getSnapshot(Long containerId) {
        return Optional.ofNullable(cache.get(containerId));
    }

    /**
     * 캐시에서 제거
     */
    public void remove(Long containerId) {
        cache.remove(containerId);
    }
}
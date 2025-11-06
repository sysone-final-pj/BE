package com.monito.global.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class FavoriteCache {

    private final ConcurrentHashMap<Long, Set<Long>> cache = new ConcurrentHashMap<>();

    /**
     * 즐겨찾기 추가
     */
    public void add(Long memberId, Long containerId) {
        cache.computeIfAbsent(memberId, k -> ConcurrentHashMap.newKeySet())
                .add(containerId);
    }

    /**
     * 즐겨찾기 제거 (Toggle 형태 시 사용)
     */
    public void remove(Long memberId, Long containerId) {
        Set<Long> set = cache.get(memberId);
        if (set != null) {
            set.remove(containerId);
        }
    }

    /**
     * 특정 사용자가 해당 컨테이너를 즐겨찾기 하고 있는지
     */
    public boolean isFavorite(Long memberId, Long containerId) {
        return cache.getOrDefault(memberId, Set.of()).contains(containerId);
    }

    /**
     * 사용자별 즐겨찾기 전체 가져오기
     */
    public Set<Long> getFavoriteSet(Long memberId) {
        return cache.getOrDefault(memberId, Set.of());
    }

    /**
     * 컨테이너가 삭제될 경우 모든 사용자 Set에서 제거
     */
    public void removeContainerFromAll(Long containerId) {
        cache.values().forEach(set -> set.remove(containerId));
    }

    /**
     * 서버 시작 시 DB에서 데이터를 로드하여 캐시 초기화
     */
    public void initialize(Long memberId, Set<Long> containerIds) {
        cache.put(memberId, ConcurrentHashMap.newKeySet());
        cache.get(memberId).addAll(containerIds);
        log.info("[FavoriteCache] 초기화 완료 - memberId: {}, 즐겨찾기 개수: {}", memberId, containerIds.size());
    }

    /**
     * 캐시 전체 초기화 (서버 시작 시 한 번만 호출)
     */
    public void clear() {
        cache.clear();
        log.info("[FavoriteCache] 캐시 전체 초기화");
    }

}

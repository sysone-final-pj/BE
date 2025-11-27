/**
 * 서버 시작 시 ContainerSummaryCache를 초기화하는 컴포넌트
 * - DB에서 모든 컨테이너와 최신 메트릭을 조회하여 캐시에 로드
 */
package com.monito.global.cache;

import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.ContainerStatsLog;
import com.monito.domains.container.dto.response.ContainerSummarySnapshot;
import com.monito.domains.container.repository.ContainerRepository;
import com.monito.domains.container.repository.ContainerStatsLogRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 작성자: 백승준
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContainerSummaryCacheInitializer {

    private final ContainerRepository containerRepository;
    private final ContainerStatsLogRepository containerStatsLogRepository;
    private final ContainerSummaryCache containerSummaryCache;

    /**
     * 서버 시작 시 ContainerSummaryCache 초기화
     */
    @PostConstruct
    @Transactional(readOnly = true)
    public void initializeContainerSummaryCache() {
        log.info("[ContainerSummaryCache] 초기화 시작");

        try {
            // 1. DB에서 모든 컨테이너 조회 (삭제되지 않은 컨테이너만)
            List<Container> allContainers = containerRepository.findAll();

            if (allContainers.isEmpty()) {
                log.info("[ContainerSummaryCache] 초기화 완료 - 컨테이너 데이터 없음");
                return;
            }

            // 2. 각 컨테이너의 최신 메트릭 조회 및 캐시에 저장
            int successCount = 0;
            for (Container container : allContainers) {
                try {
                    // 최근 1시간 내 최신 메트릭 조회 (파티션 프루닝 최적화)
                    ContainerStatsLog latestStats = containerStatsLogRepository
                            .findLatestByContainerHash(
                                    container.getContainerHash(),
                                    LocalDateTime.now().minusHours(1)
                            )
                            .orElse(null);

                    // 스냅샷 생성 및 캐시 저장
                    ContainerSummarySnapshot snapshot = ContainerSummarySnapshot.of(container, latestStats);
                    containerSummaryCache.update(snapshot);

                    successCount++;
                } catch (Exception e) {
                    log.error("[ContainerSummaryCache] 컨테이너 캐싱 실패 - containerId: {}, containerName: {}, error: {}",
                            container.getId(), container.getName(), e.getMessage());
                }
            }

            log.info("[ContainerSummaryCache] 초기화 완료 - 총 {}개 컨테이너 캐싱 성공 (전체: {}개)",
                    successCount, allContainers.size());

        } catch (Exception e) {
            log.error("[ContainerSummaryCache] 초기화 실패", e);
        }
    }
}
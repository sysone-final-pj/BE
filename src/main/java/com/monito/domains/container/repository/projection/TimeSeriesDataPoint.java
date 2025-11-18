package com.monito.domains.container.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 시계열 데이터 조회용 Projection
 * - 불필요한 컬럼을 제외하고 필요한 데이터만 조회
 * - Index-Only Scan 가능 (IDX_CONTAINER_STATS_TIMESERIES 활용)
 * - 메모리 사용량 및 DB 부하 최소화
 */
public interface TimeSeriesDataPoint {
    /**
     * 데이터 수집 시간
     */
    LocalDateTime getCollectedAt();

    /**
     * 메트릭 값 (CPU%, Memory%, Network bytes/sec, etc.)
     */
    BigDecimal getValue();
}
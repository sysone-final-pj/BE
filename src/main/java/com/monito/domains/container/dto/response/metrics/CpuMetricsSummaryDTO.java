package com.monito.domains.container.dto.response.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class CpuMetricsSummaryDTO {
    private BigDecimal current;
    private BigDecimal avg1m;
    private BigDecimal avg5m;
    private BigDecimal avg15m;
    private BigDecimal p95;
}
